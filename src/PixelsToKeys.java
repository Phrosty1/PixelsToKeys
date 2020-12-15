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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.*;
//import static liblaughlog.Utils.Log.*;

public class PixelsToKeys extends JFrame {
	int prefWidth = 300, prefHeight = 50;
	Font prefFont = new Font("Comic Sans MS", Font.BOLD, 20);
	//static String prvPxString = "", curPxString = prvPxString;
	int lastReleasedKeyCode = 0;
	static int mouseSpeedUp = 0, mouseSpeedDown = 0, mouseSpeedLeft = 0, mouseSpeedRight = 0;
	static int loopMinTime = 15;
	static int[] aKeyMap = new int[24 * 6];

	public static void main(String args[]) {

		if (false) {
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

	static int constantPixel = 0;
	static int constantLocX = 0, constantLocY = 5;
	static int[] aKeyCodeMap = new int[32];
	static ArrayList<KeyMapEntry> tKeyMapEntries = new ArrayList<>();
	static ArrayList<MouseMapEntry> tMouseMapEntries = new ArrayList<>();

	static private class KeyMapEntry {
		private final int idx, keycode;
		private final String cmdName;
		public char prvState = "0".charAt(0);

		public KeyMapEntry(int idx, int cmd, String cmdName) {
			this.idx = idx - 1;
			this.keycode = cmd;
			this.cmdName = cmdName;
		}

		public String reportIfChanged(char[] newState) {
			char cur = newState[this.idx];
			if (cur > prvState) return " Press(" + this.keycode + "):" + this.cmdName;
			else if (cur < prvState) return " Release(" + this.keycode + "):" + this.cmdName;
			return "";
		}

		public void performIfChanged(char[] newState, Robot robot) {
			char cur = newState[this.idx];
			if (cur > prvState) robot.keyPress(this.keycode);
			else if (cur < prvState) robot.keyRelease(this.keycode);
			prvState = cur;
		}
	}

	static private class MouseMapEntry {
		private final int idx, keycode;
		private final String cmdName;
		public char prvState = "0".charAt(0);

		public MouseMapEntry(int idx, int cmd, String cmdName) {
			this.idx = idx - 1;
			this.keycode = cmd;
			this.cmdName = cmdName;
		}

		public String reportIfChanged(char[] newState) {
			char cur = newState[this.idx];
			if (cur > prvState) return " Press(" + this.keycode + "):" + this.cmdName;
			else if (cur < prvState) return " Release(" + this.keycode + "):" + this.cmdName;
			return "";
		}

		public void performIfChanged(char[] newState, Robot robot) {
			char cur = newState[this.idx];
			switch (this.keycode) {
			case 1: // VM_BTN_LEFT
				if (cur > prvState) robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
				else if (cur < prvState) robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				break;
			case 2: // VM_BTN_MIDDLE
				if (cur > prvState) robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
				else if (cur < prvState) robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
				break;
			case 3: // VM_BTN_RIGHT
				if (cur > prvState) robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
				else if (cur < prvState) robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
				break;
			case 4: // VM_WHEEL_UP
				if (cur > prvState) robot.mouseWheel(1); // set this to a timer like movement
				else if (cur < prvState) robot.mouseWheel(0);
				break;
			case 5: // VM_WHEEL_DOWN
				if (cur > prvState) robot.mouseWheel(-1); // set this to a timer like movement
				else if (cur < prvState) robot.mouseWheel(0);
				break;
			case 6: // VM_MOVE_UP
				if (cur > prvState) mouseSpeedUp = 1;
				else if (cur < prvState) mouseSpeedUp = 0;
				break;
			case 7: // VM_MOVE_DOWN
				if (cur > prvState) mouseSpeedDown = 1;
				else if (cur < prvState) mouseSpeedDown = 0;
				break;
			case 8: // VM_MOVE_LEFT
				if (cur > prvState) mouseSpeedLeft = 1;
				else if (cur < prvState) mouseSpeedLeft = 0;
				break;
			case 9: // VM_MOVE_RIGHT
				if (cur > prvState) mouseSpeedRight = 1;
				else if (cur < prvState) mouseSpeedRight = 0;
				break;

			case 10: // VM_MOVE_10_UP
				if (cur > prvState) mouseSpeedUp = 10;
				else if (cur < prvState) mouseSpeedUp = 0;
				break;
			case 11: // VM_MOVE_10_DOWN
				if (cur > prvState) mouseSpeedDown = 10;
				else if (cur < prvState) mouseSpeedDown = 0;
				break;
			case 12: // VM_MOVE_10_LEFT
				if (cur > prvState) mouseSpeedLeft = 10;
				else if (cur < prvState) mouseSpeedLeft = 0;
				break;
			case 13: // VM_MOVE_10_RIGHT
				if (cur > prvState) mouseSpeedRight = 10;
				else if (cur < prvState) mouseSpeedRight = 0;
				break;
}
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
							if (val.length() > 6) val = val.substring(val.length() - 6);
							constantPixel = Integer.parseUnsignedInt("ff" + val, 16);
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
		{
			tKeyMapEntries.add(new KeyMapEntry(1, 8, "VK_BACK_SPACE"));
			tKeyMapEntries.add(new KeyMapEntry(2, 9, "VK_TAB"));
			tKeyMapEntries.add(new KeyMapEntry(3, 10, "VK_ENTER"));
			tKeyMapEntries.add(new KeyMapEntry(4, 12, "VK_CLEAR"));
			tKeyMapEntries.add(new KeyMapEntry(5, 16, "VK_SHIFT"));
			tKeyMapEntries.add(new KeyMapEntry(6, 17, "VK_CONTROL"));
			tKeyMapEntries.add(new KeyMapEntry(7, 18, "VK_ALT"));
			tKeyMapEntries.add(new KeyMapEntry(8, 19, "VK_PAUSE"));
			tKeyMapEntries.add(new KeyMapEntry(9, 20, "VK_CAPS_LOCK"));
			tKeyMapEntries.add(new KeyMapEntry(10, 27, "VK_ESCAPE"));
			tKeyMapEntries.add(new KeyMapEntry(11, 32, "VK_SPACE"));
			tKeyMapEntries.add(new KeyMapEntry(12, 33, "VK_PAGE_UP"));
			tKeyMapEntries.add(new KeyMapEntry(13, 34, "VK_PAGE_DOWN"));
			tKeyMapEntries.add(new KeyMapEntry(14, 35, "VK_END"));
			tKeyMapEntries.add(new KeyMapEntry(15, 36, "VK_HOME"));
			tKeyMapEntries.add(new KeyMapEntry(16, 37, "VK_LEFT"));
			tKeyMapEntries.add(new KeyMapEntry(17, 38, "VK_UP"));
			tKeyMapEntries.add(new KeyMapEntry(18, 39, "VK_RIGHT"));
			tKeyMapEntries.add(new KeyMapEntry(19, 40, "VK_DOWN"));
			tKeyMapEntries.add(new KeyMapEntry(20, 44, "VK_COMMA"));
			tKeyMapEntries.add(new KeyMapEntry(21, 45, "VK_MINUS"));
			tKeyMapEntries.add(new KeyMapEntry(22, 46, "VK_PERIOD"));
			tKeyMapEntries.add(new KeyMapEntry(23, 47, "VK_SLASH"));
			tKeyMapEntries.add(new KeyMapEntry(24, 48, "VK_0"));
			tKeyMapEntries.add(new KeyMapEntry(25, 49, "VK_1"));
			tKeyMapEntries.add(new KeyMapEntry(26, 50, "VK_2"));
			tKeyMapEntries.add(new KeyMapEntry(27, 51, "VK_3"));
			tKeyMapEntries.add(new KeyMapEntry(28, 52, "VK_4"));
			tKeyMapEntries.add(new KeyMapEntry(29, 53, "VK_5"));
			tKeyMapEntries.add(new KeyMapEntry(30, 54, "VK_6"));
			tKeyMapEntries.add(new KeyMapEntry(31, 55, "VK_7"));
			tKeyMapEntries.add(new KeyMapEntry(32, 56, "VK_8"));
			tKeyMapEntries.add(new KeyMapEntry(33, 57, "VK_9"));
			tKeyMapEntries.add(new KeyMapEntry(34, 59, "VK_SEMICOLON"));
			tKeyMapEntries.add(new KeyMapEntry(35, 61, "VK_EQUALS"));
			tKeyMapEntries.add(new KeyMapEntry(36, 65, "VK_A"));
			tKeyMapEntries.add(new KeyMapEntry(37, 66, "VK_B"));
			tKeyMapEntries.add(new KeyMapEntry(38, 67, "VK_C"));
			tKeyMapEntries.add(new KeyMapEntry(39, 68, "VK_D"));
			tKeyMapEntries.add(new KeyMapEntry(40, 69, "VK_E"));
			tKeyMapEntries.add(new KeyMapEntry(41, 70, "VK_F"));
			tKeyMapEntries.add(new KeyMapEntry(42, 71, "VK_G"));
			tKeyMapEntries.add(new KeyMapEntry(43, 72, "VK_H"));
			tKeyMapEntries.add(new KeyMapEntry(44, 73, "VK_I"));
			tKeyMapEntries.add(new KeyMapEntry(45, 74, "VK_J"));
			tKeyMapEntries.add(new KeyMapEntry(46, 75, "VK_K"));
			tKeyMapEntries.add(new KeyMapEntry(47, 76, "VK_L"));
			tKeyMapEntries.add(new KeyMapEntry(48, 77, "VK_M"));
			tKeyMapEntries.add(new KeyMapEntry(49, 78, "VK_N"));
			tKeyMapEntries.add(new KeyMapEntry(50, 79, "VK_O"));
			tKeyMapEntries.add(new KeyMapEntry(51, 80, "VK_P"));
			tKeyMapEntries.add(new KeyMapEntry(52, 81, "VK_Q"));
			tKeyMapEntries.add(new KeyMapEntry(53, 82, "VK_R"));
			tKeyMapEntries.add(new KeyMapEntry(54, 83, "VK_S"));
			tKeyMapEntries.add(new KeyMapEntry(55, 84, "VK_T"));
			tKeyMapEntries.add(new KeyMapEntry(56, 85, "VK_U"));
			tKeyMapEntries.add(new KeyMapEntry(57, 86, "VK_V"));
			tKeyMapEntries.add(new KeyMapEntry(58, 87, "VK_W"));
			tKeyMapEntries.add(new KeyMapEntry(59, 88, "VK_X"));
			tKeyMapEntries.add(new KeyMapEntry(60, 89, "VK_Y"));
			tKeyMapEntries.add(new KeyMapEntry(61, 90, "VK_Z"));
			tKeyMapEntries.add(new KeyMapEntry(62, 91, "VK_OPEN_BRACKET"));
			tKeyMapEntries.add(new KeyMapEntry(63, 92, "VK_BACK_SLASH"));
			tKeyMapEntries.add(new KeyMapEntry(64, 93, "VK_CLOSE_BRACKET"));
			tKeyMapEntries.add(new KeyMapEntry(65, 96, "VK_NUMPAD0"));
			tKeyMapEntries.add(new KeyMapEntry(66, 97, "VK_NUMPAD1"));
			tKeyMapEntries.add(new KeyMapEntry(67, 98, "VK_NUMPAD2"));
			tKeyMapEntries.add(new KeyMapEntry(68, 99, "VK_NUMPAD3"));
			tKeyMapEntries.add(new KeyMapEntry(69, 100, "VK_NUMPAD4"));
			tKeyMapEntries.add(new KeyMapEntry(70, 101, "VK_NUMPAD5"));
			tKeyMapEntries.add(new KeyMapEntry(71, 102, "VK_NUMPAD6"));
			tKeyMapEntries.add(new KeyMapEntry(72, 103, "VK_NUMPAD7"));
			tKeyMapEntries.add(new KeyMapEntry(73, 104, "VK_NUMPAD8"));
			tKeyMapEntries.add(new KeyMapEntry(74, 105, "VK_NUMPAD9"));
			tKeyMapEntries.add(new KeyMapEntry(75, 106, "VK_MULTIPLY"));
			tKeyMapEntries.add(new KeyMapEntry(76, 107, "VK_ADD"));
			tKeyMapEntries.add(new KeyMapEntry(77, 108, "VK_SEPARATER"));
			tKeyMapEntries.add(new KeyMapEntry(78, 108, "VK_SEPARATOR"));
			tKeyMapEntries.add(new KeyMapEntry(79, 109, "VK_SUBTRACT"));
			tKeyMapEntries.add(new KeyMapEntry(80, 110, "VK_DECIMAL"));
			tKeyMapEntries.add(new KeyMapEntry(81, 111, "VK_DIVIDE"));
			tKeyMapEntries.add(new KeyMapEntry(82, 112, "VK_F1"));
			tKeyMapEntries.add(new KeyMapEntry(83, 113, "VK_F2"));
			tKeyMapEntries.add(new KeyMapEntry(84, 114, "VK_F3"));
			tKeyMapEntries.add(new KeyMapEntry(85, 115, "VK_F4"));
			tKeyMapEntries.add(new KeyMapEntry(86, 116, "VK_F5"));
			tKeyMapEntries.add(new KeyMapEntry(87, 117, "VK_F6"));
			tKeyMapEntries.add(new KeyMapEntry(88, 118, "VK_F7"));
			tKeyMapEntries.add(new KeyMapEntry(89, 119, "VK_F8"));
			tKeyMapEntries.add(new KeyMapEntry(90, 120, "VK_F9"));
			tKeyMapEntries.add(new KeyMapEntry(91, 121, "VK_F10"));
			tKeyMapEntries.add(new KeyMapEntry(92, 122, "VK_F11"));
			tKeyMapEntries.add(new KeyMapEntry(93, 123, "VK_F12"));
			tKeyMapEntries.add(new KeyMapEntry(94, 127, "VK_DELETE"));
			tKeyMapEntries.add(new KeyMapEntry(95, 144, "VK_NUM_LOCK"));
			tKeyMapEntries.add(new KeyMapEntry(96, 145, "VK_SCROLL_LOCK"));
			tKeyMapEntries.add(new KeyMapEntry(97, 150, "VK_AMPERSAND"));
			tKeyMapEntries.add(new KeyMapEntry(98, 151, "VK_ASTERISK"));
			tKeyMapEntries.add(new KeyMapEntry(99, 152, "VK_QUOTEDBL"));
			tKeyMapEntries.add(new KeyMapEntry(100, 153, "VK_LESS"));
			tKeyMapEntries.add(new KeyMapEntry(101, 154, "VK_PRINTSCREEN"));
			tKeyMapEntries.add(new KeyMapEntry(102, 155, "VK_INSERT"));
			tKeyMapEntries.add(new KeyMapEntry(103, 160, "VK_GREATER"));
			tKeyMapEntries.add(new KeyMapEntry(104, 161, "VK_BRACELEFT"));
			tKeyMapEntries.add(new KeyMapEntry(105, 162, "VK_BRACERIGHT"));
			tKeyMapEntries.add(new KeyMapEntry(106, 192, "VK_BACK_QUOTE"));
			tKeyMapEntries.add(new KeyMapEntry(107, 222, "VK_QUOTE"));
			tKeyMapEntries.add(new KeyMapEntry(108, 224, "VK_KP_UP"));
			tKeyMapEntries.add(new KeyMapEntry(109, 225, "VK_KP_DOWN"));
			tKeyMapEntries.add(new KeyMapEntry(110, 226, "VK_KP_LEFT"));
			tKeyMapEntries.add(new KeyMapEntry(111, 227, "VK_KP_RIGHT"));
			tKeyMapEntries.add(new KeyMapEntry(112, 524, "VK_WINDOWS"));
			tMouseMapEntries.add(new MouseMapEntry(113, 1, "VM_BTN_LEFT"));
			tMouseMapEntries.add(new MouseMapEntry(114, 2, "VM_BTN_MIDDLE"));
			tMouseMapEntries.add(new MouseMapEntry(115, 3, "VM_BTN_RIGHT"));
			tMouseMapEntries.add(new MouseMapEntry(116, 4, "VM_WHEEL_UP"));
			tMouseMapEntries.add(new MouseMapEntry(117, 5, "VM_WHEEL_DOWN"));
			tMouseMapEntries.add(new MouseMapEntry(118, 6, "VM_MOVE_UP"));
			tMouseMapEntries.add(new MouseMapEntry(119, 7, "VM_MOVE_DOWN"));
			tMouseMapEntries.add(new MouseMapEntry(120, 8, "VM_MOVE_LEFT"));
			tMouseMapEntries.add(new MouseMapEntry(121, 9, "VM_MOVE_RIGHT"));
			tMouseMapEntries.add(new MouseMapEntry(122, 10, "VM_MOVE_10_UP"));
			tMouseMapEntries.add(new MouseMapEntry(123, 11, "VM_MOVE_10_DOWN"));
			tMouseMapEntries.add(new MouseMapEntry(124, 12, "VM_MOVE_10_LEFT"));
			tMouseMapEntries.add(new MouseMapEntry(125, 13, "VM_MOVE_10_RIGHT"));

}
	}

	static Runnable taskPressKeys = () -> {
		try {
			long cycleEndTime = System.currentTimeMillis(), cycleBeginTime = cycleEndTime, cycleTotTime = cycleEndTime - cycleBeginTime;
			Robot robot = new Robot();
			Rectangle rect = new Rectangle(constantLocX, constantLocY, 10, 1); // x, y, width, height
			BufferedImage tmpcap = robot.createScreenCapture(rect); // takes 15ms
			int rgb0 = tmpcap.getRGB(0, 0);//, rgb1 = tmpcap.getRGB(1, 0), rgb2 = tmpcap.getRGB(2, 0);
			String txt = "";
			String curPxBinString = "", prvPxBinString = curPxBinString;
			char[] aCurPx = curPxBinString.toCharArray(), aPrvPx = aCurPx;
			long extralogtime = 0; // remove
			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				cycleBeginTime = System.currentTimeMillis();
				tmpcap = robot.createScreenCapture(rect);
				rgb0 = tmpcap.getRGB(0, 0);
				//spLog("constantPixel:" + constantPixel + " rgb0:" + rgb0);
				if (rgb0 == constantPixel) {
					//					if (cycleBeginTime > extralogtime) { // remove
					//						spLogd("constantPixel:" + constantPixel + " rgb0:" + rgb0 + " rgb0Hex:" + Integer.toHexString(rgb0) + " rgb1:" + Integer.toBinaryString(tmpcap.getRGB(1, 0)).substring(8) + " rgb2:"
					//								+ Integer.toBinaryString(tmpcap.getRGB(2, 0)).substring(8));
					//						spLogd("curPxBinString.toCharArray:" + Arrays.toString(curPxBinString.toCharArray()));
					//						extralogtime = System.currentTimeMillis() + 500;
					//					}
					curPxBinString = Integer.toBinaryString(tmpcap.getRGB(1, 0)).substring(8)//
							+ Integer.toBinaryString(tmpcap.getRGB(2, 0)).substring(8)//
							+ Integer.toBinaryString(tmpcap.getRGB(3, 0)).substring(8)//
							+ Integer.toBinaryString(tmpcap.getRGB(4, 0)).substring(8)//
							+ Integer.toBinaryString(tmpcap.getRGB(5, 0)).substring(8)//
							+ Integer.toBinaryString(tmpcap.getRGB(6, 0)).substring(8);
					if (prvPxBinString.isEmpty()) prvPxBinString = curPxBinString; // ignore the initial state
					if (!curPxBinString.equals(prvPxBinString)) {
						txt = "\t" + "px0Hex:" + Integer.toHexString(rgb0);
						txt += "\t" + "px1Bin:" + curPxBinString;
						aCurPx = curPxBinString.toCharArray();
						for (KeyMapEntry kme : tKeyMapEntries)
							txt += kme.reportIfChanged(aCurPx);
						for (KeyMapEntry kme : tKeyMapEntries)
							kme.performIfChanged(aCurPx, robot);
						for (KeyMapEntry kme : tKeyMapEntries)
							kme.prvState = aCurPx[kme.idx];

						for (MouseMapEntry mme : tMouseMapEntries)
							txt += mme.reportIfChanged(aCurPx);
						for (MouseMapEntry mme : tMouseMapEntries)
							mme.performIfChanged(aCurPx, robot);
						for (MouseMapEntry mme : tMouseMapEntries)
							mme.prvState = aCurPx[mme.idx];

						txt += "\t" + "cycle:" + (System.currentTimeMillis() - cycleBeginTime);
						//spLogd(new StringBuilder().append(cycleTotTime).append(") ").append(txt).toString());
						spLog(txt);
						prvPxBinString = curPxBinString;
						aPrvPx = aCurPx;
					}

				}
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
