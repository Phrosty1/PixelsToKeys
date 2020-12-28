import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.*;
//import static liblaughlog.Utils.Log.*;

public class PixelsToKeys extends JFrame {
	static int constantPixel = getHexColorToInt("0x010203");
	static int constantEndPixel = getHexColorToInt("0x030201");
	static int constantLocX = 3, constantLocY = 0, cntDataPixels = 6; // constantPixel,data1,data2,data3,data4,data5,data6,constantEndPixel
	static ArrayList<KeyMapEntry> tKeyMapEntries = new ArrayList<>();
	static ArrayList<MouseMapEntry> tMouseMapEntries = new ArrayList<>();
	static ArrayList<Integer> tUnknownMapEntries = new ArrayList<>();

	int prefWidth = 300, prefHeight = 50;
	Font prefFont = new Font("Comic Sans MS", Font.BOLD, 20);
	//static String prvPxString = "", curPxString = prvPxString;
	int lastReleasedKeyCode = 0;
	static int mouseSpeedUp = 0, mouseSpeedDown = 0, mouseSpeedLeft = 0, mouseSpeedRight = 0;
	static int loopMinTime = 15;
	static final char RELEASED = "0".charAt(0), PRESSED = "1".charAt(0);

	private static void releaseAll() {
		try {
			final char[] aCharAllReleased = new char[200]; // or 1 + max idx watched in tMouseMapEntries
			for (int i = 0; i < aCharAllReleased.length; i++)
				aCharAllReleased[i] = RELEASED;
			Robot robot = new Robot();
			tKeyMapEntries.forEach(pme -> pme.performIfChanged(aCharAllReleased, robot));
			tMouseMapEntries.forEach(pme -> pme.performIfChanged(aCharAllReleased, robot));
		} catch (Exception e) {
			spLog(e.toString());
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {

		if (false) {
			spLog("begin thread call");
			new Thread(taskMouseMove).start();
			spLog("end thread call");

			try { // shortly run two commands
				Robot robot = new Robot();
				tKeyMapEntries.add(new KeyMapEntry(2, KeyEvent.VK_TAB, "VK_TAB"));
				tMouseMapEntries.add(new MouseMapEntry(121, 9, "VM_MOVE_RIGHT"));

				final char[] aTmpCharInd = new char[200];
				for (int i = 0; i < aTmpCharInd.length; i++)
					aTmpCharInd[i] = RELEASED;
				spLog("begin");

				aTmpCharInd[2 - 1] = PRESSED;
				aTmpCharInd[121 - 1] = PRESSED;

				tKeyMapEntries.forEach(pme -> spLog(pme.reportIfChanged(aTmpCharInd)));
				tKeyMapEntries.forEach(pme -> pme.performIfChanged(aTmpCharInd, robot));
				tKeyMapEntries.forEach(pme -> pme.prvState = aTmpCharInd[pme.idx]);
				tMouseMapEntries.forEach(pme -> spLog(pme.reportIfChanged(aTmpCharInd)));
				tMouseMapEntries.forEach(pme -> pme.performIfChanged(aTmpCharInd, robot));
				tMouseMapEntries.forEach(pme -> pme.prvState = aTmpCharInd[pme.idx]);

				spLog("before delay");
				robot.delay(100);
				spLog("after delay");

				aTmpCharInd[2 - 1] = RELEASED;
				aTmpCharInd[121 - 1] = RELEASED;

				tKeyMapEntries.forEach(pme -> spLog(pme.reportIfChanged(aTmpCharInd)));
				tKeyMapEntries.forEach(pme -> pme.performIfChanged(aTmpCharInd, robot));
				tKeyMapEntries.forEach(pme -> pme.prvState = aTmpCharInd[pme.idx]);
				tMouseMapEntries.forEach(pme -> spLog(pme.reportIfChanged(aTmpCharInd)));
				tMouseMapEntries.forEach(pme -> pme.performIfChanged(aTmpCharInd, robot));
				tMouseMapEntries.forEach(pme -> pme.prvState = aTmpCharInd[pme.idx]);

			} catch (Exception e) {
				e.printStackTrace();
				spLog(e.toString());
			}
			spLog("done");

			return;
		}

		if (false) { // list out the constants
			Field[] fields = java.awt.event.KeyEvent.class.getDeclaredFields();
			for (Field f : fields) {
				if (Modifier.isStatic(f.getModifiers())) {
					try {
						System.out.println(f.getInt(f.getName()) + "\t" + f.getName());
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
			return;
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new PixelsToKeys().setVisible(true);
			}
		});
		loadKeyMap();

		spLog("begin thread call");
		new Thread(taskPressKeys).start();
		new Thread(taskMouseMove).start();
		spLog("end thread call");
	}

	static private class KeyMapEntry {
		private final int idx, keycode;
		private final String cmdName;
		public char prvState = RELEASED; // "0".charAt(0);

		public KeyMapEntry(int idx, int cmd, String cmdName) {
			this.idx = idx;
			this.keycode = cmd;
			this.cmdName = cmdName;
		}

		public String reportIfChanged(char[] newState) {
			char cur = newState[this.idx];
			if (prvState == RELEASED & cur == PRESSED) return " Press(" + this.keycode + "):" + this.cmdName;
			else if (prvState == PRESSED & cur == RELEASED) return " Release(" + this.keycode + "):" + this.cmdName;
			return "";
		}

		public void performIfChanged(char[] newState, Robot robot) {
			char cur = newState[this.idx];
			try {
				if (prvState == RELEASED & cur == PRESSED) robot.keyPress(this.keycode);
				else if (prvState == PRESSED & cur == RELEASED) robot.keyRelease(this.keycode);
			} catch (Exception e) {
				//spLog(e.toString());//532144) java.lang.IllegalArgumentException: Invalid key code
				spLog(e.toString() + " - idx:" + this.idx + " cmdName:" + this.cmdName);
				e.printStackTrace();
			}
			prvState = cur;
		}
	}

	static private class MouseMapEntry {
		private final int idx, keycode;
		private final String cmdName;
		public char prvState = RELEASED; // "0".charAt(0);

		public MouseMapEntry(int idx, int cmd, String cmdName) {
			this.idx = idx;
			this.keycode = cmd;
			this.cmdName = cmdName;
		}

		public String reportIfChanged(char[] newState) {
			char cur = newState[this.idx];
			if (prvState == RELEASED & cur == PRESSED) return " Press(" + this.keycode + "):" + this.cmdName;
			else if (prvState == PRESSED & cur == RELEASED) return " Release(" + this.keycode + "):" + this.cmdName;
			return "";
		}

		public void performIfChanged(char[] newState, Robot robot) {

			char cur = newState[this.idx];
			try {
				switch (this.keycode) {
				case 1: // VM_BTN_LEFT
					if (prvState == RELEASED & cur == PRESSED) robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					else if (prvState == PRESSED & cur == RELEASED) robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					break;
				case 2: // VM_BTN_MIDDLE
					if (prvState == RELEASED & cur == PRESSED) robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					else if (prvState == PRESSED & cur == RELEASED) robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					break;
				case 3: // VM_BTN_RIGHT
					if (prvState == RELEASED & cur == PRESSED) robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
					else if (prvState == PRESSED & cur == RELEASED) robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					break;
				case 4: // VM_WHEEL_UP
					if (prvState == RELEASED & cur == PRESSED) robot.mouseWheel(1); // set this to a timer like movement
					else if (prvState == PRESSED & cur == RELEASED) robot.mouseWheel(0);
					break;
				case 5: // VM_WHEEL_DOWN
					if (prvState == RELEASED & cur == PRESSED) robot.mouseWheel(-1); // set this to a timer like movement
					else if (prvState == PRESSED & cur == RELEASED) robot.mouseWheel(0);
					break;
				case 6: // VM_MOVE_UP
					if (prvState == RELEASED & cur == PRESSED) mouseSpeedUp = 1;
					else if (prvState == PRESSED & cur == RELEASED) mouseSpeedUp = 0;
					break;
				case 7: // VM_MOVE_DOWN
					if (prvState == RELEASED & cur == PRESSED) mouseSpeedDown = 1;
					else if (prvState == PRESSED & cur == RELEASED) mouseSpeedDown = 0;
					break;
				case 8: // VM_MOVE_LEFT
					if (prvState == RELEASED & cur == PRESSED) mouseSpeedLeft = 1;
					else if (prvState == PRESSED & cur == RELEASED) mouseSpeedLeft = 0;
					break;
				case 9: // VM_MOVE_RIGHT
					if (prvState == RELEASED & cur == PRESSED) mouseSpeedRight = 1;
					else if (prvState == PRESSED & cur == RELEASED) mouseSpeedRight = 0;
					break;
				case 10: // VM_MOVE_10_UP
					if (prvState == RELEASED & cur == PRESSED) mouseSpeedUp = 10;
					else if (prvState == PRESSED & cur == RELEASED) mouseSpeedUp = 0;
					break;
				case 11: // VM_MOVE_10_DOWN
					if (prvState == RELEASED & cur == PRESSED) mouseSpeedDown = 10;
					else if (prvState == PRESSED & cur == RELEASED) mouseSpeedDown = 0;
					break;
				case 12: // VM_MOVE_10_LEFT
					if (prvState == RELEASED & cur == PRESSED) mouseSpeedLeft = 10;
					else if (prvState == PRESSED & cur == RELEASED) mouseSpeedLeft = 0;
					break;
				case 13: // VM_MOVE_10_RIGHT
					if (prvState == RELEASED & cur == PRESSED) mouseSpeedRight = 10;
					else if (prvState == PRESSED & cur == RELEASED) mouseSpeedRight = 0;
					break;
				}
			} catch (Exception e) {
				//spLog(e.toString());//532144) java.lang.IllegalArgumentException: Invalid key code
				spLog(e.toString() + " - idx:" + this.idx + " cmdName:" + this.cmdName);
				e.printStackTrace();
			}
			prvState = cur;
		}
	}

	private static String getRegSubstr(String haystack, String needle) {
		String retval = "";
		Pattern pattern = Pattern.compile(needle + ".*");
		Matcher matcher = pattern.matcher(haystack);
		if (matcher.matches()) retval = matcher.group(1);
		return retval;

	}

	private static String getMouseCoordsString() {
		Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
		return new StringBuilder().append("(").append(mouseLoc.x).append(",").append(mouseLoc.y).append(")").toString();
	}

	private static int getHexColorToInt(String color) {
		String val = color;
		if (val.length() > 6) val = val.substring(val.length() - 6);
		return Integer.parseUnsignedInt("ff" + val, 16);
	}

	private static void loadKeyMap() {
		// ConstantPixel = 0x010203
		// ConstantLocX = 0
		// ConstantLocY = 5
		// KeyMapValues = 87 w, 83 s, 65 a, 68 d, 69 e, 88 x
		// MouseMapValues = mousebtn1, mousebtn2, mousebtn3, leftmouse, upspeed1, downspeed1, leftspeed1, rightspeed1
		File mapFile = new File("PixelsToKeys.txt");
		if (!mapFile.exists()) {
			File dir = new File((new File("")).getAbsolutePath());
			File[] aFiles = dir.listFiles((dir1, name) -> name.endsWith(".config"));
			if (aFiles.length > 0) mapFile = aFiles[0];
		}
		spLog("Config File = " + mapFile.getName());
		if (mapFile.exists()) {
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(mapFile)))) {
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					spLog(line);
					if (line.contains("=")) {
						String field = line.substring(1, line.indexOf("=") + 1).trim();
						if (line.toUpperCase().trim().startsWith("ConstantPixel".toUpperCase())) {
							String val = line.substring(line.indexOf("=") + 1).trim();
							val = getRegSubstr(val, "(\\w+)"); // word
							//if (val.length() > 6) val = val.substring(val.length() - 6);
							//constantPixel = Integer.parseUnsignedInt("ff" + val, 16);
							constantPixel = getHexColorToInt(val);
							spLog("ConstantPixel=" + constantPixel);
						} else if (line.toUpperCase().trim().startsWith("ConstantLocX".toUpperCase())) {
							String val = line.substring(line.indexOf("=") + 1).trim();
							val = getRegSubstr(val, "(\\d+)"); // digit
							if (!val.isEmpty()) constantLocX = Integer.parseInt(val);
							spLog("ConstantLocX=" + constantLocX);
						} else if (line.toUpperCase().trim().startsWith("ConstantLocY".toUpperCase())) {
							String val = line.substring(line.indexOf("=") + 1).trim();
							val = getRegSubstr(val, "(\\d+)"); // digit
							if (!val.isEmpty()) constantLocY = Integer.parseInt(val);
							spLog("ConstantLocY=" + constantLocY);
						} else if (line.toUpperCase().trim().startsWith("KeyMapValues".toUpperCase())) {
							//
						} else if (line.toUpperCase().trim().startsWith("MouseMapValues".toUpperCase())) {
							//
						}
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		tKeyMapEntries.clear();
		tMouseMapEntries.clear();
		tUnknownMapEntries.clear();
		{
			tKeyMapEntries.add(new KeyMapEntry(0, KeyEvent.VK_BACK_SPACE, "VK_BACK_SPACE"));
			tKeyMapEntries.add(new KeyMapEntry(1, KeyEvent.VK_TAB, "VK_TAB"));
			tKeyMapEntries.add(new KeyMapEntry(2, KeyEvent.VK_ENTER, "VK_ENTER"));
			tKeyMapEntries.add(new KeyMapEntry(3, KeyEvent.VK_CLEAR, "VK_CLEAR"));
			tKeyMapEntries.add(new KeyMapEntry(4, KeyEvent.VK_SHIFT, "VK_SHIFT"));
			tKeyMapEntries.add(new KeyMapEntry(5, KeyEvent.VK_CONTROL, "VK_CONTROL"));
			tKeyMapEntries.add(new KeyMapEntry(6, KeyEvent.VK_ALT, "VK_ALT"));
			tKeyMapEntries.add(new KeyMapEntry(7, KeyEvent.VK_PAUSE, "VK_PAUSE"));
			tKeyMapEntries.add(new KeyMapEntry(8, KeyEvent.VK_CAPS_LOCK, "VK_CAPS_LOCK"));
			tKeyMapEntries.add(new KeyMapEntry(9, KeyEvent.VK_ESCAPE, "VK_ESCAPE"));
			tKeyMapEntries.add(new KeyMapEntry(10, KeyEvent.VK_SPACE, "VK_SPACE"));
			tKeyMapEntries.add(new KeyMapEntry(11, KeyEvent.VK_PAGE_UP, "VK_PAGE_UP"));
			tKeyMapEntries.add(new KeyMapEntry(12, KeyEvent.VK_PAGE_DOWN, "VK_PAGE_DOWN"));
			tKeyMapEntries.add(new KeyMapEntry(13, KeyEvent.VK_END, "VK_END"));
			tKeyMapEntries.add(new KeyMapEntry(14, KeyEvent.VK_HOME, "VK_HOME"));
			tKeyMapEntries.add(new KeyMapEntry(15, KeyEvent.VK_LEFT, "VK_LEFT"));
			tKeyMapEntries.add(new KeyMapEntry(16, KeyEvent.VK_UP, "VK_UP"));
			tKeyMapEntries.add(new KeyMapEntry(17, KeyEvent.VK_RIGHT, "VK_RIGHT"));
			tKeyMapEntries.add(new KeyMapEntry(18, KeyEvent.VK_DOWN, "VK_DOWN"));
			tKeyMapEntries.add(new KeyMapEntry(19, KeyEvent.VK_COMMA, "VK_COMMA"));
			tKeyMapEntries.add(new KeyMapEntry(20, KeyEvent.VK_MINUS, "VK_MINUS"));
			tKeyMapEntries.add(new KeyMapEntry(21, KeyEvent.VK_PERIOD, "VK_PERIOD"));
			tKeyMapEntries.add(new KeyMapEntry(22, KeyEvent.VK_SLASH, "VK_SLASH"));
			tKeyMapEntries.add(new KeyMapEntry(23, KeyEvent.VK_0, "VK_0"));
			tKeyMapEntries.add(new KeyMapEntry(24, KeyEvent.VK_1, "VK_1"));
			tKeyMapEntries.add(new KeyMapEntry(25, KeyEvent.VK_2, "VK_2"));
			tKeyMapEntries.add(new KeyMapEntry(26, KeyEvent.VK_3, "VK_3"));
			tKeyMapEntries.add(new KeyMapEntry(27, KeyEvent.VK_4, "VK_4"));
			tKeyMapEntries.add(new KeyMapEntry(28, KeyEvent.VK_5, "VK_5"));
			tKeyMapEntries.add(new KeyMapEntry(29, KeyEvent.VK_6, "VK_6"));
			tKeyMapEntries.add(new KeyMapEntry(30, KeyEvent.VK_7, "VK_7"));
			tKeyMapEntries.add(new KeyMapEntry(31, KeyEvent.VK_8, "VK_8"));
			tKeyMapEntries.add(new KeyMapEntry(32, KeyEvent.VK_9, "VK_9"));
			tKeyMapEntries.add(new KeyMapEntry(33, KeyEvent.VK_SEMICOLON, "VK_SEMICOLON"));
			tKeyMapEntries.add(new KeyMapEntry(34, KeyEvent.VK_EQUALS, "VK_EQUALS"));
			tKeyMapEntries.add(new KeyMapEntry(35, KeyEvent.VK_A, "VK_A"));
			tKeyMapEntries.add(new KeyMapEntry(36, KeyEvent.VK_B, "VK_B"));
			tKeyMapEntries.add(new KeyMapEntry(37, KeyEvent.VK_C, "VK_C"));
			tKeyMapEntries.add(new KeyMapEntry(38, KeyEvent.VK_D, "VK_D"));
			tKeyMapEntries.add(new KeyMapEntry(39, KeyEvent.VK_E, "VK_E"));
			tKeyMapEntries.add(new KeyMapEntry(40, KeyEvent.VK_F, "VK_F"));
			tKeyMapEntries.add(new KeyMapEntry(41, KeyEvent.VK_G, "VK_G"));
			tKeyMapEntries.add(new KeyMapEntry(42, KeyEvent.VK_H, "VK_H"));
			tKeyMapEntries.add(new KeyMapEntry(43, KeyEvent.VK_I, "VK_I"));
			tKeyMapEntries.add(new KeyMapEntry(44, KeyEvent.VK_J, "VK_J"));
			tKeyMapEntries.add(new KeyMapEntry(45, KeyEvent.VK_K, "VK_K"));
			tKeyMapEntries.add(new KeyMapEntry(46, KeyEvent.VK_L, "VK_L"));
			tKeyMapEntries.add(new KeyMapEntry(47, KeyEvent.VK_M, "VK_M"));
			tKeyMapEntries.add(new KeyMapEntry(48, KeyEvent.VK_N, "VK_N"));
			tKeyMapEntries.add(new KeyMapEntry(49, KeyEvent.VK_O, "VK_O"));
			tKeyMapEntries.add(new KeyMapEntry(50, KeyEvent.VK_P, "VK_P"));
			tKeyMapEntries.add(new KeyMapEntry(51, KeyEvent.VK_Q, "VK_Q"));
			tKeyMapEntries.add(new KeyMapEntry(52, KeyEvent.VK_R, "VK_R"));
			tKeyMapEntries.add(new KeyMapEntry(53, KeyEvent.VK_S, "VK_S"));
			tKeyMapEntries.add(new KeyMapEntry(54, KeyEvent.VK_T, "VK_T"));
			tKeyMapEntries.add(new KeyMapEntry(55, KeyEvent.VK_U, "VK_U"));
			tKeyMapEntries.add(new KeyMapEntry(56, KeyEvent.VK_V, "VK_V"));
			tKeyMapEntries.add(new KeyMapEntry(57, KeyEvent.VK_W, "VK_W"));
			tKeyMapEntries.add(new KeyMapEntry(58, KeyEvent.VK_X, "VK_X"));
			tKeyMapEntries.add(new KeyMapEntry(59, KeyEvent.VK_Y, "VK_Y"));
			tKeyMapEntries.add(new KeyMapEntry(60, KeyEvent.VK_Z, "VK_Z"));
			tKeyMapEntries.add(new KeyMapEntry(61, KeyEvent.VK_OPEN_BRACKET, "VK_OPEN_BRACKET"));
			tKeyMapEntries.add(new KeyMapEntry(62, KeyEvent.VK_BACK_SLASH, "VK_BACK_SLASH"));
			tKeyMapEntries.add(new KeyMapEntry(63, KeyEvent.VK_CLOSE_BRACKET, "VK_CLOSE_BRACKET"));
			tKeyMapEntries.add(new KeyMapEntry(64, KeyEvent.VK_NUMPAD0, "VK_NUMPAD0"));
			tKeyMapEntries.add(new KeyMapEntry(65, KeyEvent.VK_NUMPAD1, "VK_NUMPAD1"));
			tKeyMapEntries.add(new KeyMapEntry(66, KeyEvent.VK_NUMPAD2, "VK_NUMPAD2"));
			tKeyMapEntries.add(new KeyMapEntry(67, KeyEvent.VK_NUMPAD3, "VK_NUMPAD3"));
			tKeyMapEntries.add(new KeyMapEntry(68, KeyEvent.VK_NUMPAD4, "VK_NUMPAD4"));
			tKeyMapEntries.add(new KeyMapEntry(69, KeyEvent.VK_NUMPAD5, "VK_NUMPAD5"));
			tKeyMapEntries.add(new KeyMapEntry(70, KeyEvent.VK_NUMPAD6, "VK_NUMPAD6"));
			tKeyMapEntries.add(new KeyMapEntry(71, KeyEvent.VK_NUMPAD7, "VK_NUMPAD7"));
			tKeyMapEntries.add(new KeyMapEntry(72, KeyEvent.VK_NUMPAD8, "VK_NUMPAD8"));
			tKeyMapEntries.add(new KeyMapEntry(73, KeyEvent.VK_NUMPAD9, "VK_NUMPAD9"));
			tKeyMapEntries.add(new KeyMapEntry(74, KeyEvent.VK_MULTIPLY, "VK_MULTIPLY"));
			tKeyMapEntries.add(new KeyMapEntry(75, KeyEvent.VK_ADD, "VK_ADD"));
			tKeyMapEntries.add(new KeyMapEntry(76, KeyEvent.VK_SEPARATER, "VK_SEPARATER"));
			tKeyMapEntries.add(new KeyMapEntry(77, KeyEvent.VK_SEPARATOR, "VK_SEPARATOR"));
			tKeyMapEntries.add(new KeyMapEntry(78, KeyEvent.VK_SUBTRACT, "VK_SUBTRACT"));
			tKeyMapEntries.add(new KeyMapEntry(79, KeyEvent.VK_DECIMAL, "VK_DECIMAL"));
			tKeyMapEntries.add(new KeyMapEntry(80, KeyEvent.VK_DIVIDE, "VK_DIVIDE"));
			tKeyMapEntries.add(new KeyMapEntry(81, KeyEvent.VK_F1, "VK_F1"));
			tKeyMapEntries.add(new KeyMapEntry(82, KeyEvent.VK_F2, "VK_F2"));
			tKeyMapEntries.add(new KeyMapEntry(83, KeyEvent.VK_F3, "VK_F3"));
			tKeyMapEntries.add(new KeyMapEntry(84, KeyEvent.VK_F4, "VK_F4"));
			tKeyMapEntries.add(new KeyMapEntry(85, KeyEvent.VK_F5, "VK_F5"));
			tKeyMapEntries.add(new KeyMapEntry(86, KeyEvent.VK_F6, "VK_F6"));
			tKeyMapEntries.add(new KeyMapEntry(87, KeyEvent.VK_F7, "VK_F7"));
			tKeyMapEntries.add(new KeyMapEntry(88, KeyEvent.VK_F8, "VK_F8"));
			tKeyMapEntries.add(new KeyMapEntry(89, KeyEvent.VK_F9, "VK_F9"));
			tKeyMapEntries.add(new KeyMapEntry(90, KeyEvent.VK_F10, "VK_F10"));
			tKeyMapEntries.add(new KeyMapEntry(91, KeyEvent.VK_F11, "VK_F11"));
			tKeyMapEntries.add(new KeyMapEntry(92, KeyEvent.VK_F12, "VK_F12"));
			tKeyMapEntries.add(new KeyMapEntry(93, KeyEvent.VK_DELETE, "VK_DELETE"));
			tKeyMapEntries.add(new KeyMapEntry(94, KeyEvent.VK_NUM_LOCK, "VK_NUM_LOCK"));
			tKeyMapEntries.add(new KeyMapEntry(95, KeyEvent.VK_SCROLL_LOCK, "VK_SCROLL_LOCK"));
			tKeyMapEntries.add(new KeyMapEntry(96, KeyEvent.VK_AMPERSAND, "VK_AMPERSAND"));
			tKeyMapEntries.add(new KeyMapEntry(97, KeyEvent.VK_ASTERISK, "VK_ASTERISK"));
			tKeyMapEntries.add(new KeyMapEntry(98, KeyEvent.VK_QUOTEDBL, "VK_QUOTEDBL"));
			tKeyMapEntries.add(new KeyMapEntry(99, KeyEvent.VK_LESS, "VK_LESS"));
			tKeyMapEntries.add(new KeyMapEntry(100, KeyEvent.VK_PRINTSCREEN, "VK_PRINTSCREEN"));
			tKeyMapEntries.add(new KeyMapEntry(101, KeyEvent.VK_INSERT, "VK_INSERT"));
			tKeyMapEntries.add(new KeyMapEntry(102, KeyEvent.VK_GREATER, "VK_GREATER"));
			tKeyMapEntries.add(new KeyMapEntry(103, KeyEvent.VK_BRACELEFT, "VK_BRACELEFT"));
			tKeyMapEntries.add(new KeyMapEntry(104, KeyEvent.VK_BRACERIGHT, "VK_BRACERIGHT"));
			tKeyMapEntries.add(new KeyMapEntry(105, KeyEvent.VK_BACK_QUOTE, "VK_BACK_QUOTE"));
			tKeyMapEntries.add(new KeyMapEntry(106, KeyEvent.VK_QUOTE, "VK_QUOTE"));
			tKeyMapEntries.add(new KeyMapEntry(107, KeyEvent.VK_KP_UP, "VK_KP_UP"));
			tKeyMapEntries.add(new KeyMapEntry(108, KeyEvent.VK_KP_DOWN, "VK_KP_DOWN"));
			tKeyMapEntries.add(new KeyMapEntry(109, KeyEvent.VK_KP_LEFT, "VK_KP_LEFT"));
			tKeyMapEntries.add(new KeyMapEntry(110, KeyEvent.VK_KP_RIGHT, "VK_KP_RIGHT"));
			tKeyMapEntries.add(new KeyMapEntry(111, KeyEvent.VK_WINDOWS, "VK_WINDOWS"));
			tMouseMapEntries.add(new MouseMapEntry(112, 1, "VM_BTN_LEFT"));
			tMouseMapEntries.add(new MouseMapEntry(113, 2, "VM_BTN_MIDDLE"));
			tMouseMapEntries.add(new MouseMapEntry(114, 3, "VM_BTN_RIGHT"));
			tMouseMapEntries.add(new MouseMapEntry(115, 4, "VM_WHEEL_UP"));
			tMouseMapEntries.add(new MouseMapEntry(116, 5, "VM_WHEEL_DOWN"));
			tMouseMapEntries.add(new MouseMapEntry(117, 6, "VM_MOVE_UP"));
			tMouseMapEntries.add(new MouseMapEntry(118, 7, "VM_MOVE_DOWN"));
			tMouseMapEntries.add(new MouseMapEntry(119, 8, "VM_MOVE_LEFT"));
			tMouseMapEntries.add(new MouseMapEntry(120, 9, "VM_MOVE_RIGHT"));
			tMouseMapEntries.add(new MouseMapEntry(121, 10, "VM_MOVE_10_UP"));
			tMouseMapEntries.add(new MouseMapEntry(122, 11, "VM_MOVE_10_DOWN"));
			tMouseMapEntries.add(new MouseMapEntry(123, 12, "VM_MOVE_10_LEFT"));
			tMouseMapEntries.add(new MouseMapEntry(124, 13, "VM_MOVE_10_RIGHT"));
		}
		{
			for (int i = 0; i < cntDataPixels * 24; i++) 
				tUnknownMapEntries.add(i);
			Collections.sort(tKeyMapEntries, Comparator.comparing(pme -> pme.idx));
			Collections.sort(tMouseMapEntries, Comparator.comparing(pme -> pme.idx));
			Collections.sort(tUnknownMapEntries);

			ArrayList<KeyMapEntry> tKeyMap = tKeyMapEntries;
			ArrayList<MouseMapEntry> tMouseMap = tMouseMapEntries;
			ArrayList<Integer> tKnownMap = new ArrayList<>();

			for (KeyMapEntry pme : tKeyMapEntries) 
				tKnownMap.add(pme.idx);
			for (MouseMapEntry pme : tMouseMapEntries) 
				tKnownMap.add(pme.idx);
			tUnknownMapEntries.removeAll(tKnownMap);
		}

	}

	static Runnable taskPressKeys = () -> {
		try {
			long cycleEndTime = System.currentTimeMillis(), cycleBeginTime = cycleEndTime, cycleTotTime = cycleEndTime - cycleBeginTime;
			Robot robot = new Robot();
			Rectangle rect = new Rectangle(constantLocX, constantLocY, 10, 1); // x, y, width, height
			BufferedImage tmpcap = robot.createScreenCapture(rect); // takes about 15ms / operates at roughly 58fps
			int prvRGB0 = tmpcap.getRGB(0, 0);
			int prvRGB1 = tmpcap.getRGB(1, 0);
			int prvRGB2 = tmpcap.getRGB(2, 0);
			int prvRGB3 = tmpcap.getRGB(3, 0);
			int prvRGB4 = tmpcap.getRGB(4, 0);
			int prvRGB5 = tmpcap.getRGB(5, 0);
			int prvRGB6 = tmpcap.getRGB(6, 0);
			int prvRGB7 = tmpcap.getRGB(7, 0);
			int cntConsistent = 0;
			//long extralogtime = 0; // remove
			String txt = "";
			String curPxBinString = "", prvPxBinString = curPxBinString;
			ArrayList<String> tPreviousInds = new ArrayList<>();
			while (true) {
				cycleBeginTime = System.currentTimeMillis();
				tmpcap = robot.createScreenCapture(rect);
				int rgb0 = tmpcap.getRGB(0, 0);
				int rgb1 = tmpcap.getRGB(1, 0);
				int rgb2 = tmpcap.getRGB(2, 0);
				int rgb3 = tmpcap.getRGB(3, 0);
				int rgb4 = tmpcap.getRGB(4, 0);
				int rgb5 = tmpcap.getRGB(5, 0);
				int rgb6 = tmpcap.getRGB(6, 0);
				int rgb7 = tmpcap.getRGB(7, 0);
				curPxBinString = Integer.toBinaryString(rgb1).substring(8)//
						+ Integer.toBinaryString(rgb2).substring(8)//
						+ Integer.toBinaryString(rgb3).substring(8)//
						+ Integer.toBinaryString(rgb4).substring(8)//
						+ Integer.toBinaryString(rgb5).substring(8)//
						+ Integer.toBinaryString(rgb6).substring(8);
				tPreviousInds.add(Integer.toHexString(rgb0) + " " + curPxBinString + " " + Integer.toHexString(rgb7));
				while (tPreviousInds.size() > 10)
					tPreviousInds.remove(0);
				//spLog("constantPixel:" + constantPixel + " rgb0:" + rgb0);
				if (rgb0 == constantPixel & rgb7 == constantEndPixel) { // Once we've had both constants AND all 0 for 10 checks, we can start looking for pixels.
					if ((rgb1 + rgb2 + rgb3 + rgb4 + rgb5 + rgb6) == -100663296) cntConsistent++;
					else if (rgb2 == rgb1 & rgb3 == rgb1 & rgb4 == rgb1 & rgb5 == rgb1 & rgb6 == rgb1) { // faceroll check
						// reset the constant counter if all are off 0 by the same amount
						for (String prvTxt : tPreviousInds)
							spLogd(prvTxt);
						spLog("All are off 0 by the same amount. Releasing all and resetting.");
						cntConsistent = 0;
					}
				} else cntConsistent = 0;
				char[] aCurPx = curPxBinString.toCharArray();
				
				if (cntConsistent > 10) { // faceroll check
					for (Integer unkIdx : tUnknownMapEntries) {
						if (aCurPx[unkIdx] == PRESSED) {
							for (String prvTxt : tPreviousInds)
								spLogd(prvTxt);
							spLog("Unknown Index " + unkIdx + " is set. Releasing all and resetting.");
							cntConsistent = 0;
						}
					}
				}
				if (cntConsistent > 10) { // faceroll check
					int cntKeysSet = 0;
					for (char c : aCurPx)
						if (c == PRESSED) cntKeysSet++;
					if (cntKeysSet >= 5) {
						for (String prvTxt : tPreviousInds)
							spLogd(prvTxt);
						spLog("Too many indicators set (" + cntKeysSet + "). Releasing all and resetting.");
						cntConsistent = 0;
					}
				}

				if (cntConsistent > 10) {
					//if (cycleBeginTime > extralogtime) { // remove
					//	spLogd("constantPixel:" + constantPixel + " rgb0:" + rgb0 + //
					//			"constantEndPixel:" + constantEndPixel + " rgbEnd:" + rgb7 + //
					//			" rgb0Hex:" + Integer.toHexString(rgb0) + " rgb1:" + Integer.toBinaryString(tmpcap.getRGB(1, 0)).substring(8) + " rgb2:" + Integer.toBinaryString(tmpcap.getRGB(2, 0)).substring(8));
					//	spLogd("curPxBinString.toCharArray:" + Arrays.toString(curPxBinString.toCharArray()));
					//	extralogtime = System.currentTimeMillis() + 500;
					//}
					if (prvPxBinString.isEmpty()) prvPxBinString = curPxBinString; // ignore the initial state
					if (!curPxBinString.equals(prvPxBinString)) {
						txt = "\t" + "px0Hex:" + Integer.toHexString(rgb0);
						txt += "\t" + "px7Hex:" + Integer.toHexString(rgb7);
						txt += "\t" + "px1Bin:" + curPxBinString;
						for (KeyMapEntry pme : tKeyMapEntries) {
							txt += pme.reportIfChanged(aCurPx);
							pme.performIfChanged(aCurPx, robot);
							//	{
							//		char cur = aCurPx[pme.idx];
							//		if (pme.prvState == RELEASED & cur == PRESSED) robot.keyPress(pme.keycode);
							//		else if (pme.prvState == PRESSED & cur == RELEASED) robot.keyRelease(pme.keycode);
							//		pme.prvState = cur;
							//	}
							pme.prvState = aCurPx[pme.idx];
						}
						for (MouseMapEntry pme : tMouseMapEntries) {
							txt += pme.reportIfChanged(aCurPx);
							pme.performIfChanged(aCurPx, robot);
							pme.prvState = aCurPx[pme.idx];
						}
						txt += "\t" + "cycle:" + (System.currentTimeMillis() - cycleBeginTime);
						//spLogd(new StringBuilder().append(cycleTotTime).append(") ").append(txt).toString());
						spLog(txt);
						prvPxBinString = curPxBinString;
					}

				} else releaseAll();
				cycleEndTime = System.currentTimeMillis();
				cycleTotTime = cycleEndTime - cycleBeginTime;
				if (cycleTotTime < loopMinTime) Thread.sleep(loopMinTime - cycleTotTime);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	};

	static Runnable taskMouseMove = () -> {
		try {
			while (true) {
				if (mouseSpeedUp != 0 | mouseSpeedDown != 0 | mouseSpeedLeft != 0 | mouseSpeedRight != 0) {
					int moveX = (mouseSpeedRight - mouseSpeedLeft), moveY = (mouseSpeedDown - mouseSpeedUp);
					Robot robot = new Robot(MouseInfo.getPointerInfo().getDevice());
					Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
					robot.mouseMove(mouseLoc.x + moveX, mouseLoc.y + moveY);
				}
				Thread.sleep(10);
			}
		} catch (AWTException | InterruptedException e) {
			e.printStackTrace();
		}
	};

	public PixelsToKeys() {
		spLog("begin PixelsToKeys");
		initComponents();
		spLog("end PixelsToKeys");
	}

	private void initComponents() {
		jPanel2 = new Panel2();
		jPanel2.setBackground(new java.awt.Color(255, 255, 255));
		jPanel2.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		jPanel2.setFocusable(true);
		jPanel2.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent arg0) {
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				lastReleasedKeyCode = arg0.getKeyCode();
				spLogd("keyReleased: " + arg0.getKeyCode());
				repaint();
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
			}
		});
		this.setContentPane(jPanel2); // add the component to the frame to see it!
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // be nice to testers..
		pack();

		spLog("end initComponents");
	}

	private JPanel jPanel2;

	class Panel2 extends JPanel {
		Panel2() {
			setPreferredSize(new Dimension(prefWidth, prefHeight)); // set a preferred size for the custom panel.
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			int canvasWidth = (int) g.getClipBounds().getWidth(), canvasHeight = (int) g.getClipBounds().getHeight();
			spLogd("paintComponent: " + canvasWidth + " x " + canvasHeight);
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, canvasWidth, canvasHeight);
			g.setColor(Color.BLACK);
			g.setFont(prefFont);
			g.drawString("lastReleasedKeyCode:" + lastReleasedKeyCode, 10, 30);

		}
	}

	//------------------
	public static long msLogTime = System.currentTimeMillis();
	private static StringBuilder sbLog = new StringBuilder();
	private static FileWriter fwLogFileWriter;

	public synchronized static String resetlogtimer() {
		msLogTime = System.currentTimeMillis();
		return "";
	}

	public synchronized static String sfLog(String txt) {
		spLogd(txt);
		return txt;
	}

	public synchronized static void spLog(String txt) {
		long nowTime = System.currentTimeMillis();
		spLogd((nowTime - msLogTime) + ") " + txt);
		//spLogd(txt);
		msLogTime = nowTime;
	}

	public synchronized static void spLogd(String txt) {
		sbLog.append(txt).append("\n");
		System.out.println(txt);
		if (fwLogFileWriter == null) {
			try {
				fwLogFileWriter = new FileWriter("PixelsToKeys.log", false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		PrintWriter outWrite = new PrintWriter(fwLogFileWriter);
		outWrite.println(txt);
		outWrite.flush();
	}

}
