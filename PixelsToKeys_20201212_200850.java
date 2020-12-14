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
	static int onlyWhenHoldingKeyCode = 0;
	static int loopMinTime = 15;

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
		private final int idx;
		private final int keycode;

		public KeyMapEntry(int idx, int cmd) {
			this.idx = idx;
			this.keycode = cmd;
		}
	}

	static private class MouseMapEntry {
		private final int idx;
		private final String cmdType;
		private final int cmdDigit;

		public MouseMapEntry(int idx, String cmd, int val) {
			this.idx = idx;
			this.cmdType = cmd;
			this.cmdDigit = val;
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
						} else if (line.toUpperCase().trim().startsWith("OnlyWhenHoldingKeyCode".toUpperCase())) {
							String val = line.substring(line.indexOf("=") + 1).trim();
							val = getRegSubstr(val, "(\\d+)"); // digit
							if (!val.isEmpty()) onlyWhenHoldingKeyCode = Integer.parseInt(val);
							spLog("OnlyWhenHoldingKeyCode=" + onlyWhenHoldingKeyCode);
						} else if (line.toUpperCase().trim().startsWith("KeyMapValues".toUpperCase())) {
							String val = line.substring(line.indexOf("=") + 1).trim();
							String[] aVals = val.split(",");
							int j = 0;
							for (String eVal : aVals) {
								String cmdDigit = getRegSubstr(eVal.trim(), "(\\d+)"); // digit
								if (!cmdDigit.isEmpty()) tKeyMapEntries.add(new KeyMapEntry(j, Integer.parseInt(cmdDigit)));
								spLog(j + " cmdDigit=" + cmdDigit);
								j++;
							}
						} else if (line.toUpperCase().trim().startsWith("MouseMapValues".toUpperCase())) {
							String val = line.substring(line.indexOf("=") + 1).trim();
							String[] aVals = val.split(",");
							int j = 0;
							for (String eVal : aVals) {
								String cmdType = getRegSubstr(eVal.toLowerCase().trim(), "([a-z]+)"); // letters
								String cmdDigit = getRegSubstr(eVal.trim(), ".*(\\d+)"); // digit
								if (!cmdType.isEmpty()) {
									int cmdDigitVal = 0;
									if (!cmdDigit.isEmpty()) cmdDigitVal = Integer.parseInt(cmdDigit);
									if (cmdType.compareToIgnoreCase("mousebtn") == 0) {
										if (cmdDigitVal == 1) cmdDigitVal = InputEvent.BUTTON1_DOWN_MASK;
										else if (cmdDigitVal == 2) cmdDigitVal = InputEvent.BUTTON2_DOWN_MASK;
										else if (cmdDigitVal == 3) cmdDigitVal = InputEvent.BUTTON3_DOWN_MASK;
									}
									tMouseMapEntries.add(new MouseMapEntry(j, cmdType, cmdDigitVal));
								}
								spLog(j + " cmdType=" + cmdType + " cmdDigit=" + cmdDigit);
								j++;
							}
						}
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static Runnable taskPressKeys = () -> {
		try {
			long cycleEndTime = System.currentTimeMillis(), cycleBeginTime = cycleEndTime, cycleTotTime = cycleEndTime - cycleBeginTime;
			Robot robot = new Robot();
			Rectangle rect = new Rectangle(constantLocX, constantLocY, 3, 1); // x, y, width, height
			BufferedImage tmpcap = robot.createScreenCapture(rect); // takes 15ms
			int rgb0 = tmpcap.getRGB(0, 0), rgb1 = tmpcap.getRGB(1, 0), rgb2 = tmpcap.getRGB(2, 0);
			String txt = "";
			String curPxBinString = "000000000000000000000000", prvPxBinString = curPxBinString;
			char[] aCurPx = curPxBinString.toCharArray(), aPrvPx = aCurPx;
			String curMousePxBinString = "000000000000000000000000", prvMousePxBinString = curMousePxBinString;
			char[] aCurMousePx = curPxBinString.toCharArray(), aPrvMousePx = aCurMousePx;
			long extralogtime = 0; // remove
			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				cycleBeginTime = System.currentTimeMillis();
				tmpcap = robot.createScreenCapture(rect);
				rgb0 = tmpcap.getRGB(0, 0);
				//spLog("constantPixel:" + constantPixel + " rgb0:" + rgb0);
				if (rgb0 == constantPixel) {
					if (cycleBeginTime > extralogtime) { // remove
						spLogd("constantPixel:" + constantPixel + " rgb0:" + rgb0 + " rgb0Hex:" + Integer.toHexString(rgb0) + " rgb1:" + Integer.toBinaryString(tmpcap.getRGB(1, 0)).substring(8) + " rgb2:"
								+ Integer.toBinaryString(tmpcap.getRGB(2, 0)).substring(8));
						extralogtime = System.currentTimeMillis() + 500;
					}
					rgb1 = tmpcap.getRGB(1, 0);
					curPxBinString = Integer.toBinaryString(rgb1).substring(8);
					rgb2 = tmpcap.getRGB(2, 0);
					curMousePxBinString = Integer.toBinaryString(rgb2).substring(8);

					if (!curPxBinString.equals(prvPxBinString)) {
						txt = "\t" + "px0Hex:" + Integer.toHexString(rgb0);
						txt += "\t" + "px1Bin:" + curPxBinString;
						aCurPx = curPxBinString.toCharArray();
						for (KeyMapEntry kme : tKeyMapEntries) {
							if (kme.keycode > 0) {
								char cur = aCurPx[kme.idx], prv = aPrvPx[kme.idx];
								if (cur > prv) {
									robot.keyPress(kme.keycode);
									txt += " Press:" + kme.keycode;
								} else if (cur < prv) {
									robot.keyRelease(kme.keycode);
									txt += " Release:" + kme.keycode;
								}
							}
						}

						txt += "\t" + "cycle:" + (System.currentTimeMillis() - cycleBeginTime);
						//spLogd(new StringBuilder().append(cycleTotTime).append(") ").append(txt).toString());
						spLog(txt);
						prvPxBinString = curPxBinString;
						aPrvPx = aCurPx;
					}

					if (!curMousePxBinString.equals(prvMousePxBinString)) {
						txt = "";
						txt += "\t" + "curMousePxBinString:" + curMousePxBinString;
						txt += "\t" + getMouseCoordsString();
						aCurMousePx = curMousePxBinString.toCharArray();
						for (MouseMapEntry mme : tMouseMapEntries) {
							char cur = aCurMousePx[mme.idx], prv = aPrvMousePx[mme.idx];
							if (mme.cmdType.equalsIgnoreCase("mousebtn")) {
								if (cur > prv) {
									robot.mousePress(mme.cmdDigit);
									txt += " MousePress:" + mme.cmdDigit;
								} else if (cur < prv) {
									robot.mouseRelease(mme.cmdDigit);
									txt += " MouseRelease:" + mme.cmdDigit;
								}
							} else if (mme.cmdType.equalsIgnoreCase("upspeed")) {
								if (cur > prv) {
									mouseSpeedUp = -mme.cmdDigit;
									txt += " mouseSpeedUp:" + mouseSpeedUp;
								} else if (cur < prv) {
									mouseSpeedUp = 0;
									txt += " mouseSpeedUp:" + mouseSpeedUp;
								}
							} else if (mme.cmdType.equalsIgnoreCase("downspeed")) {
								if (cur > prv) {
									mouseSpeedDown = -mme.cmdDigit;
									txt += " mouseSpeedDown:" + mouseSpeedDown;
								} else if (cur < prv) {
									mouseSpeedDown = 0;
									txt += " mouseSpeedDown:" + mouseSpeedDown;
								}
							} else if (mme.cmdType.equalsIgnoreCase("leftspeed")) {
								if (cur > prv) {
									mouseSpeedLeft = -mme.cmdDigit;
									txt += " mouseSpeedLeft:" + mouseSpeedLeft;
								} else if (cur < prv) {
									mouseSpeedLeft = 0;
									txt += " mouseSpeedLeft:" + mouseSpeedLeft;
								}
							} else if (mme.cmdType.equalsIgnoreCase("rightspeed")) {
								if (cur > prv) {
									mouseSpeedRight = -mme.cmdDigit;
									txt += " mouseSpeedRight:" + mouseSpeedRight;
								} else if (cur < prv) {
									mouseSpeedRight = 0;
									txt += " mouseSpeedRight:" + mouseSpeedRight;
								}
							}
						}

						txt += "\t" + "cycle:" + (System.currentTimeMillis() - cycleBeginTime);
						//spLogd(new StringBuilder().append(cycleTotTime).append(") ").append(txt).toString());
						spLog(txt);
						prvMousePxBinString = curMousePxBinString;
						aPrvMousePx = aCurMousePx;
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
