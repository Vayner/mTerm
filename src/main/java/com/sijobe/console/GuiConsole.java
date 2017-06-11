package com.sijobe.console;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.src.ChatAllowedCharacters;
import net.minecraft.src.ChatLine;
import net.minecraft.src.EntityClientPlayerMP;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.GuiIngame;

import net.minecraft.src.GuiPlayerInfo;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NetClientHandler;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.mod_Console;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.vayner.console.ConsoleChatCommands;
import com.vayner.console.external.ExternalGuiConsole;

import net.minecraft.src.GuiModScreen;
import com.vayner.console.guiapi.ConsoleSettings;

/**
 * @formatter:off
 *                TODO: P1 - Only save logs for the current world
 *                TODO: p1 - Per world / server configuration file
 *                TODO: P1 - Output filtering - allow blocking of certain text/people
 *                DONE: p2 - Text selection in the chat-history field (copy text)
 *                DONE: P2 - Spinner (tab auto complete) (more or less)
 *                TODO: P3 - Drop down menus?
 *                TODO: P2 - Improve look and feel
 *                TODO: P2 - Custom text color support. Holding CTRL then type a number will set the text to that color [0-f] - (0-15)
 *                TODO: P1 - Add ability to disable settings loader (in code) and ability to reset the settings ingame
 *                TODO: P3 - Dynamic settings screen, configure any setting in an easy to use GUI (partly complete)
 *                TODO: p2 - Improve text highlighting to be less buggy
 *                DONE: p1 - Add external window / console
 *                TODO: p1 - Add tab completion to external console
 *                TODO: p2 - Rewrite / improve text highlight system
 *                DONE: p1 - Fix message splitting incorrectly
 *                FIXME:p2 - Add option for 1 tick unpause - pause for singleplayer.
 *
 * @author simo_415, tellefma.
 *
 *         This program is free software: you can redistribute it and/or modify
 *         it under the terms of the GNU Lesser General Public License as published by
 *         the Free Software Foundation, either version 3 of the License, or
 *         (at your option) any later version.
 *
 *         This program is distributed in the hope that it will be useful,
 *         but WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *         GNU Lesser General Public License for more details.
 *
 *         You should have received a copy of the GNU General Public License
 *         along with this program. If not, see <http://www.gnu.org/licenses/>.
 * @formatter:on
 */
public class GuiConsole extends GuiScreen implements Runnable {
   /* @formatter:off */
   private final String playername;                                  // The name of the current player
   protected String message;                                         // The current user input
   protected String input;                                           // The current input line to draw (includes prefix)
   private int updateCounter;                                        // The tick count - used for cursor blink rate
   private int slider;                                               // Position of the scroll bar
   private int cursor;                                               // Position of the cursor
   private int inputOffset;                                          // Position in the message string where the input goes
   private int sliderHeight;                                         // Height of the scroll bar
   private int currentChatWidth = 128;                               // Current chat space width
   private boolean isHighlighting;                                   // Keeps track of the highlight mouse click
   private int[] firstHighlighting = new int[2];                     // Position of the mouse (at character) initially for highlighting
   private int[] lastHighlighting = new int[2];                      // Position of the mouse (at character) at end of highlighting
   private boolean isSliding;                                        // Keeps track of the slider mouse click
   private int lastSliding;                                          // Position of mouse at last frame for slider
   private int initialSliding;                                       // Position of mouse initially for slider
   private int historyPosition;                                      // Position of where in the history you are at
   private boolean isGuiOpen;                                        // When the console is open this is true
   private boolean rebuildLines;                                     // Keeps track of whether the lines list needs to be rebuilt
   private volatile Vector<String> log;                              // The log messages
   private SimpleDateFormat sdf;                                     // The date format for logs

   private boolean pauseGame = true;
   private int pauseCountDown = 0;
   
   private int tabListPos;                                           // Where you have tabbed to through word list
   private int tabMaxPos;                                            // Max size of the list
   private boolean tabbing = false;                                  // Is tabbing
   private boolean tabMatchPlayerNamesOnly = false;                  // Is matching for player names
   private int tabWordPos;                                           // start place of tabWord
   private String tabMatchingWord;                                   // The current word checking to
   private String tabMatchedWord;                                    // The current word matched to
   private String tabBeforeCursor;                                   // The string before the cursor when completing
   private String tabAfterCursor;                                    // The string after the cursor when completing
   private ArrayList<String> tabCurrentList;                         // The current List matching words
   
   private volatile HashMap<String,String> keyBindings;              // All the current key bindings
   private volatile List<Integer> keyDown;                           // List of all the keys currently held down
   private static boolean BACKGROUND_BINDING_EVENTS = false;         // Allows the bindings to run ingame with different GUIs open

   private String logName;                                           // The name of the log file to write
   private long lastWrite;                                           // The time of the last log write
   
   private static final int CHARHEIGHT = 10;                               // Character height - used to quickly determine number of lines per view
   private static final String ALLOWED_CHARACTERS;                   // A list of permitted characters

   public static Vector<String> INPUT_HISTORY;                       // All the input which went into the console
   private static Vector<String> LINES;                              // All of the lines to output
   private static Vector<String> MESSAGES;                           // All of the input/output
   private static Vector<ConsoleListener> LISTENERS;                 // All of the console listeners which were registered

   private static int[] TOP;                                         // Poor implementation to keep track of drawn scrollbar top button
   private static int[] BOTTOM;                                      // Poor implementation to keep track of drawn scrollbar bottom button
   private static int[] BAR;                                         // Poor implementation to keep track of drawn scrollbar
   private static int[] EXIT_BUTTON;                                 // Poor implementation to keep track of drawn exit button
   private static int[] OPTION_BUTTON;                               // Poor implementation to keep track of drawn option button
   private static int[] EXTERNAL_BUTTON;                             // Poor implementation to keep track of drawn external console button
   private static int[] TEXT_BOX;                                    // Poor implementation to keep track of drawn text box
   private static int[] HISTORY;                                     // Poor implementation to keep track of drawn history field
   
   
   private static int CHAT_INPUT_LENGTH_MAX = 150;                   // Maximum input size on the console
   private static int CHAT_INPUT_LENGTH_SERVER_MAX = 100;            // Maximum server message size - splits the input to this length if it is longer
   private static int CHAT_INPUT_HISTORY_MAX = 50;                   // Maximum size of stored input history
   private static int CHAT_OUTPUT_MAX = 200;                              // Maximum number of lines in the output
   
   private static boolean CHAT_UNPASUE_PAUSE_WITH_MESSAGE = true;   // TODO
   private static boolean CHAT_PRINT_INPUT = true;                   // Prints the input
   private static boolean CHAT_PRINT_OUTPUT = true;                  // Prints the output
   private static String CHAT_INPUT_PREFIX = "> ";                   // Prefix for all input messages
   
   private static boolean CLOSE_ON_SUBMIT = false;                   // Closes the GUI after the input has been submit
   private static boolean SCROLL_TO_BOTTOM_ON_SUBMIT = true;         // Moves the scroll bar to the bottom when input is sumbitted
   private static boolean CLOSE_WITH_OPEN_KEY = true;                // Closes the GUI if the open key pressed again

   public static final byte LOGGING_TRACE  = 8;                      // Logging level - Trace
   public static final byte LOGGING_DEBUG  = 4;                      // Logging level - Debug
   public static final byte LOGGING_INPUT  = 2;                      // Logging level - Input
   public static final byte LOGGING_OUTPUT = 1;                      // Logging level - Output
   private static int LOGGING = LOGGING_INPUT + LOGGING_OUTPUT;      // What is currently being logged
   private static long LOG_WRITE_INTERVAL = 1000L;                   // How often (in ms) the logs are written to file
   
   // The log line separator
   private static final String LINE_BREAK = System.getProperty("line.separator");
   private static final Pattern VALID_MESSAGE = Pattern.compile("\\S");
   
   private static String DATE_FORMAT_LOG = "yyyy-MM-dd hh:mm:ss: ";  // The date format according to SimpleDateFormat
   // The date format filename (uses SimpleDateFormat)
   private static String DATE_FORMAT_FILENAME = "yyyyMMdd_hhmmss'.log'";

   private static long POLL_DELAY = 20L;                             // The amount of time (in ms) to run the thread at

   private static int SCREEN_LINES_PER_SCROLL = 1;                          // The number of lines to scroll in one scroll wheel click

   private static int SCREEN_BORDERSIZE = 2;                         // Size of the border
   private static int SCREEN_PADDING_LEFT = 5;                       // Size of the screen padding - left
   private static int SCREEN_PADDING_TOP = 12;                       // Size of the screen padding - top
   private static int SCREEN_PADDING_RIGHT = 5;                      // Size of the screen padding - right
   private static int SCREEN_PADDING_BOTTOM = 40;                    // Size of the screen padding - bottom

   private static boolean SCREEN_MESSAGE_LENGHT_DISPLAY = true;      // Turn on or off showing chars left for 1 message
   private static boolean SCREEN_AUTOPREVIEW = true;                 // Turn on or off previewing matched words
   private static int SCREEN_AUTOPREVIEWAREA = 140;                  // width of preview area
   
   private static int COLOR_BASE = 0x90000000;                       // Base colour to use for console
   private static int COLOR_SCROLL_BACKGROUND = 0xBB999999;          // Scroll background colour
   private static int COLOR_SCROLL_FOREGROUND = 0xBB404040;          // Scroll foreground colour
   private static int COLOR_INPUT_TEXT = 0xFFE0E0E0;                 // Colour of the input text
   private static int COLOR_TEXT_OUTPUT = 0xFFE0E0E0;                // Colour of the text output
   private static int COLOR_TEXT_TITLE = 0xFFE0E0E0;                 // Colour of the text title
   private static int COLOR_TEXT_HIGHLIGHT = 0xFF2090DD;             // Colour of the text highlighting
   private static int COLOR_SCROLL_ARROW = 0xFFFFFFFF;               // Colour of the scroll arrow
   private static int COLOR_EXIT_BUTTON_TEXT = 0xFFFFFFFF;           // Colour of the exit button label
   private static int COLOR_EXIT_BUTTON = 0xBB999999;                // Colour of the exit button
   private static int COLOR_OUTPUT_BACKGROUND = 0xBB999999;          // Colour of the output background
   private static int COLOR_INPUT_BACKGROUND = 0xBB999999;           // Colour of the input background
   private static int COLOR_MESSAGE_LENGTH_BACKGROUND = 0xBB797979;  // Colour of the message length background
   
   private static boolean MISC_PASUE_GAME = true;                    // Game is pasued or not when the ocnsole is open
   
   public static final String VERSION = "1.3.6.2";                   // Version of the mod  
   private static String TITLE = "Console";                          // Title of the console

   private static final String MOD_PATH = "mods/console/";           // Relative location of the mod directory
   private static String LOG_PATH = "mods/console/logs";             // Relative location of the console logs
   
   private static File MOD_DIR = new File(Minecraft.getMinecraftDir(), MOD_PATH);    // Mod directory
   private static File LOG_DIR = new File(Minecraft.getMinecraftDir(), LOG_PATH);   // Log directory
   private static File GUI_SETTINGS_FILE = new File(MOD_DIR, "gui.properties");
   private static File GUI_SETTINGS_DEFAULT_FILE = new File(MOD_DIR, "gui-default.properties");

   private static GuiConsole INSTANCE;                               // Instance of the class for singleton pattern
   private static ArrayList<Field> defaultSettings;
   
   private static boolean EMACS_KEYS = false;                        // Use emacs keybindings
   
   private static int KEY_AUTOCOMPLETE = Keyboard.KEY_TAB;           // Autocomlete keybinding
   private static int KEY_AUTOPREV = Keyboard.KEY_LEFT;              // Next match
   private static int KEY_AUTONEXT = Keyboard.KEY_RIGHT;             // Previous match

   /* @formatter:on */

   /**
    * Initialises all of the instance variables
    */
   static {
      if(!LOG_DIR.exists())
         LOG_DIR.mkdirs();
      
      if (!GUI_SETTINGS_DEFAULT_FILE.exists())
         writeSettings(GuiConsole.class, GUI_SETTINGS_DEFAULT_FILE);
      
      if (GUI_SETTINGS_FILE.exists())
         readSettings(GuiConsole.class, GUI_SETTINGS_FILE);
      
      writeSettings(GuiConsole.class, GUI_SETTINGS_FILE);
      
      ALLOWED_CHARACTERS = ChatAllowedCharacters.allowedCharacters;
      MESSAGES = new Vector<String>();
      MESSAGES.add("\2476[MCC] Minecraft Console version: \2473" + VERSION + "\2476 for Minecraft version: \24731.4.4");
      MESSAGES.add("\2476Developers: \2472simo_415 \2476, \2474fsmv \2476and \2471tellefma");
      MESSAGES.add("");
      INPUT_HISTORY = new Vector<String>();
      LISTENERS = new Vector<ConsoleListener>();
      
      TOP               = new int[4];
      BOTTOM            = new int[4];
      BAR               = new int[4];
      EXIT_BUTTON       = new int[4];
      OPTION_BUTTON     = new int[4];
      EXTERNAL_BUTTON   = new int[4];
      TEXT_BOX          = new int[4];
      
      INSTANCE = new GuiConsole();
      
      if (!MOD_DIR.exists()) {
         try {
            MOD_DIR.mkdirs();
         } catch (Exception e) {
         }
      }
      
      if (!LOG_DIR.exists()) {
         try {
            LOG_DIR.mkdirs();
         } catch (Exception e) {
         }
      }
   }

   /**
    * Constructor should only be initialised from within the class ( currently via static{ } )
    */
   private GuiConsole() {
      mc = ModLoader.getMinecraftInstance();
      playername = mc.session.username;
      (new Thread(this)).start();
      isGuiOpen = false;
      log = new Vector<String>();
      sdf = new SimpleDateFormat(DATE_FORMAT_LOG);
      keyBindings = generateKeyBindings();
      keyDown = new Vector<Integer>();
      
      loadCoreCommands();
   }

   /**
    * Loads the core set of classes which handle player input/output.
    */
   private void loadCoreCommands() {
      addConsoleListener(new ConsoleSettingCommands());
      addConsoleListener(new ConsoleChatCommands());
      addConsoleListener(new ExternalGuiConsole());
   }

   /**
    * SingleTon pattern to get an instance of the GUI
    *
    * @return An instance of the GUI
    */
   public static GuiConsole getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new GuiConsole();
      }
      return INSTANCE;
   }
   
   /**
    * returns the current directory Minecraft console saves it files.
    * 
    * @return Minecraft console current mod directory
    */
   public static File getModDir() {
      return MOD_DIR;
   }
   
   /**
    * Generates a hashmap containing all of the configured key bindings from file
    *
    * @return A hashmap containing all the keybindings
    */
   public HashMap<String, String> generateKeyBindings() {
      Properties p = new Properties();
      HashMap<String, String> bindings = new HashMap<String, String>();
      try {
         p.load(new FileInputStream(new File(MOD_DIR, "bindings.properties")));
         Iterator i = p.keySet().iterator();
         while (i.hasNext()) {
            String o = (String) i.next();
            bindings.put(o, (String) p.get(o));
         }
      } catch (FileNotFoundException e) {
         System.out.println("[MCC] Could not find bindings.properties in " + MOD_DIR.getAbsolutePath());
      } catch (IOException e) {
         ModLoader.throwException(e);
      }
      return bindings;
   }

   /**
    * Rebuilds the line list so that the text input and slider can be correctly
    * rendered without missing parts and dynamically resize if required.
    */
   public void buildLines() {
      LINES = new Vector<String>();
      Vector<String> temp = (Vector<String>) MESSAGES.clone(); //There were problems with the run method modifying MESSAGES while this loop works. See issue #17
      for (String message : temp) {
         addLine(message);
      }
      rebuildLines = false;
   }

   /**
    * Adds a line to the line render list
    *
    * @param message - The line message to add
    */
   private void addLine(String message) {
      
      if (LINES == null) {
         buildLines();
      }

      if (message == null) {
         return;
      }
      
      //using minecraft's methods instead, seems to work fine for the moment
      LINES.addAll(mc.fontRenderer.listFormattedStringToWidth(message, currentChatWidth));
      
   }

   /**
    * Called when Minecraft initialises the GUI
    *
    * @see net.minecraft.src.GuiScreen#initGui()
    */
   @Override
   public void initGui() {
      Keyboard.enableRepeatEvents(true);
      isSliding = false;
      lastSliding = -1;
      slider = 0;
      initialSliding = 0;
      isHighlighting = false;
      clearHighlighting();
      cursor = 0;
      message = "";
      updateCounter = 0;
      historyPosition = 0;
      isGuiOpen = true;
      rebuildLines = true;
   }

   /**
    * Called when the GUI is closed by Minecraft - useful for cleanup
    *
    * @see net.minecraft.src.GuiScreen#onGuiClosed()
    */
   @Override
   public void onGuiClosed() {
      Keyboard.enableRepeatEvents(false);
      isGuiOpen = false;
   }
   
   /**
    * Called to update the screen on frame
    *
    * @see net.minecraft.src.GuiScreen#updateScreen()
    */
   @Override
   public void updateScreen() {
      updateCounter++;
   }

   
   @Override
   public boolean doesGuiPauseGame() {
      if(MISC_PASUE_GAME && CHAT_UNPASUE_PAUSE_WITH_MESSAGE && !pauseGame) {
         pauseGame = (pauseCountDown-- <= 0)? true : false;
         return false;
      }
      return MISC_PASUE_GAME;
   }
   
   /**
    * Called when a key is typed, handles all input into the console
    *
    * @see net.minecraft.src.GuiScreen#keyTyped(char, int)
    */
   @Override
   protected void keyTyped(char key, int id) {
      // Multi key validation
      // Control + ?
      if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
         if (Keyboard.isKeyDown(Keyboard.KEY_C)) {
            if (firstHighlighting[0] != -1 && lastHighlighting[0] != -1) {
               String clipboard = "";
               int firstInLINES = firstHighlighting[0] <= lastHighlighting[0] ? firstHighlighting[0] : lastHighlighting[0];

               if (firstHighlighting[0] == lastHighlighting[0]) {
                  int firsti, lasti;
                  if (firstHighlighting[1] < lastHighlighting[1]) {
                     firsti = firstHighlighting[1];
                     lasti = lastHighlighting[1];
                  } else {
                     firsti = lastHighlighting[1];
                     lasti = firstHighlighting[1];
                  }
                  clipboard = LINES.get(firstInLINES).substring(firsti, lasti);
               } else {
                  for (int i = 0; i < Math.abs(firstHighlighting[0] - lastHighlighting[0]); i++) {
                     String temp = LINES.get(firstInLINES + i);
                     if (firstInLINES + i == firstHighlighting[0]) {
                        if (firstHighlighting[0] < lastHighlighting[0]) {
                           temp = temp.substring(firstHighlighting[1]);
                        } else {
                           temp = temp.substring(0, firstHighlighting[1]);
                        }
                     } else if (firstInLINES + i == lastHighlighting[0]) {
                        if (firstHighlighting[0] > lastHighlighting[0]) {
                           temp = temp.substring(lastHighlighting[1]);
                        } else {
                           temp = temp.substring(0, lastHighlighting[1]);
                        }
                     }

                     clipboard += temp + " ";
                  }
               }

               setClipboardString(clipboard.trim());
            } else {
               if (lastHighlighting[1] != firstHighlighting[1] && firstHighlighting[0] == -1 && lastHighlighting[0] == -1) {
                  if (firstHighlighting[1] < lastHighlighting[1]) {
                     setClipboardString(message.substring(firstHighlighting[1], lastHighlighting[1]));
                  } else {
                     setClipboardString(message.substring(lastHighlighting[1], firstHighlighting[1]));
                  }
               }
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
            paste();
         } else if (Keyboard.isKeyDown(Keyboard.KEY_X)) {
            if (firstHighlighting[1] != lastHighlighting[1] && firstHighlighting[0] == -1 && lastHighlighting[0] == -1) {
               String start, end;
               if (firstHighlighting[1] < lastHighlighting[1]) {
                  setClipboardString(message.substring(firstHighlighting[1], lastHighlighting[1]));
                  start = message.substring(0, firstHighlighting[1]);
                  end = message.substring(lastHighlighting[1]);
               } else {
                  setClipboardString(message.substring(lastHighlighting[1], firstHighlighting[1]));
                  start = message.substring(0, lastHighlighting[1]);
                  end = message.substring(firstHighlighting[1]);
               }

               message = start + end;
               firstHighlighting[0] = lastHighlighting[0] = -1;
               firstHighlighting[1] = lastHighlighting[1] = 0;
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            if (!EMACS_KEYS) {
               if (firstHighlighting[0] == -1 && lastHighlighting[0] == -1) {
                  firstHighlighting[1] = 0;
                  lastHighlighting[1] = message.length();
               } else {
                  firstHighlighting[0] = 0;
                  firstHighlighting[1] = 0;
                  lastHighlighting[0] = LINES.size() - 1;
                  lastHighlighting[1] = LINES.get(LINES.size() - 1).length();
               }
            } else {
               // go to beginning of line
               cursor = 0;
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
            if (EMACS_KEYS) {
               // go to end of line
               cursor = message.length();
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_K)) {
            if (EMACS_KEYS && firstHighlighting[0] == -1 && lastHighlighting[0] == -1) {
               // Cut to end of line
               setClipboardString(message.substring(cursor, message.length()));

               message = message.substring(0, cursor);
               clearHighlighting();
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_Y)) {
            if (EMACS_KEYS) {
               paste();
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            if (EMACS_KEYS) {
               delete();
            }
         }
         return;
      }
      
      if(tabbing)
      {
         if(id == KEY_AUTONEXT)
         {
            updateTabPos(1);
            return;
         }
         else if(id == KEY_AUTOPREV)
         {
            updateTabPos(-1);
            return;
         }
      }
      
      if (id != KEY_AUTOCOMPLETE && id != KEY_AUTONEXT && id != KEY_AUTOPREV && id != Keyboard.KEY_BACK) {
         resetTabbing();
      }
      else if(id == KEY_AUTOCOMPLETE)
      {
         clearHighlighting();
         /*if (message.startsWith("@get ") || message.startsWith("@list ") || message.startsWith("@set ")) {
            String[] str = message.split(" ");

            String match = "";

            if (str.length > 1) {
               if (tabListPos == 0) {
                  match = str[1];
               } else {
                  match = tabMatchingWord;
               }
            }
            if (tabListPos < 0) {
               tabListPos = 0;
            }

            if (cursor >= str[0].length() + 1 && cursor <= str[0].length() + 1 + match.length() || tabListPos > 0) {
               ArrayList<String> tempList = new ArrayList<String>(Arrays.asList(ConsoleSettingCommands.list("").split("\n")));
               ArrayList<String> list = new ArrayList<String>();
               for (int i = 0; i < tempList.size(); i++) {
                  if (tempList.get(i).startsWith(match.toUpperCase())) {
                     list.add(tempList.get(i)); //Can't delete from a list in a loop; workaround
                  }
               }

               if (list.size() > 0) {
                  tabMatchingWord = match;

                  if (tabListPos == 0) {
                     message = message.substring(0, str[0].length() + 1) + list.get(tabListPos) + message.substring(str[0].length() + 1 + match.length(), message.length());
                  } else if (tabListPos > 0) {
                     message = message.substring(0, str[0].length() + 1) + list.get(tabListPos) + message.substring(str[0].length() + 1 + list.get(tabListPos - 1).length(), message.length());
                  }
                  cursor = str[0].length() + 1 + list.get(tabListPos).length();

                  tabListPos++;
                  if (tabListPos >= list.size()) {
                     tabListPos = -1;
                  }
               }
            }
         } else {*/
            updateTabPos(1);
         //}
         return;
      }

      // Single key validation
      switch (id) {
         case Keyboard.KEY_ESCAPE:
            // Exits the GUI
            mc.displayGuiScreen(null);
            break;

         case Keyboard.KEY_RETURN:
            // Submits the message
            String s = message.trim();

            if (s.length() > 0) {
               addInputMessage(s);
            }

            if (CLOSE_ON_SUBMIT) {
               mc.displayGuiScreen(null);
            }

            if (SCROLL_TO_BOTTOM_ON_SUBMIT) {
               slider = 0;
            }

            message = "";
            cursor = 0;
            inputOffset = 0;
            historyPosition = 0;
            clearHighlighting();
            resetTabbing();
            break;

         case Keyboard.KEY_LEFT:
            // Moves the cursor left
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
               if (firstHighlighting[1] == lastHighlighting[1]) {
                  if (firstHighlighting[0] == -1 && lastHighlighting[0] == -1) {
                     firstHighlighting[1] = cursor;
                     lastHighlighting[1] = cursor;
                  }
               }

               lastHighlighting[1]--;
               if (lastHighlighting[1] < 0) {
                  lastHighlighting[1] = 0;
                  if (lastHighlighting[0] > 0) {
                     lastHighlighting[0]--;
                  }
               }
               validateHighlighting();
            } else {
               clearHighlighting();
            }
            cursor--;
            break;

         case Keyboard.KEY_RIGHT:
            // Moves the cursor right
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
               if (firstHighlighting[1] == lastHighlighting[1]) {
                  if (firstHighlighting[0] == -1 && lastHighlighting[0] == -1) {
                     firstHighlighting[1] = cursor;
                     lastHighlighting[1] = cursor;
                  }
               }

               lastHighlighting[1]++;
               if (lastHighlighting[0] == -1 && firstHighlighting[0] == -1) {
                  if (lastHighlighting[1] > message.length()) {
                     lastHighlighting[1] = message.length();
                  }
               } else {
                  if (lastHighlighting[1] > LINES.get(lastHighlighting[0]).length()) {
                     lastHighlighting[1] = LINES.get(lastHighlighting[0]).length();
                     if (lastHighlighting[0] < LINES.size() - 1) {
                        lastHighlighting[0]++;
                     }
                  }
               }
               validateHighlighting();
            } else {
               clearHighlighting();
            }
            cursor++;
            break;

         case Keyboard.KEY_DOWN:
            // Moves the history position down
            message = getInputHistory(--historyPosition);
            cursor = message.length();
            clearHighlighting();
            break;

         case Keyboard.KEY_UP:
            // Moves the history position down
            message = getInputHistory(++historyPosition);
            cursor = message.length();
            clearHighlighting();
            break;

         case Keyboard.KEY_DELETE:
            resetTabbing();
            delete();
            break;

         case Keyboard.KEY_BACK:
            // Backspace
            if(tabbing) {
               message = message.substring(0,tabWordPos) + tabMatchingWord;
               resetTabbing();
               break;
            }
            if (message.length() > 0) {
               if (firstHighlighting[1] == lastHighlighting[1] || firstHighlighting[0] != -1 || lastHighlighting[0] != -1) {
                  validateCursor();
                  String start = message.substring(0, cursor);
                  String end = message.substring(cursor, message.length());
                  this.message = start.substring(0, (start.length() - 1 > -1 ? start.length() - 1 : 0)) + end;
                  cursor--;
                  inputOffset--;
                  if (inputOffset < 0) {
                     inputOffset = 0;
                  }
               } else {
                  String start, end;
                  if (firstHighlighting[1] < lastHighlighting[1]) {
                     start = message.substring(0, firstHighlighting[1]);
                     end = message.substring(lastHighlighting[1]);
                  } else {
                     start = message.substring(0, lastHighlighting[1]);
                     end = message.substring(firstHighlighting[1]);
                  }
                  inputOffset -= Math.abs(lastHighlighting[1] - firstHighlighting[1]);
                  if (inputOffset < 0) {
                     inputOffset = 0;
                  }
                  message = start + end;
               }
               clearHighlighting();
            }
            break;
         case Keyboard.KEY_HOME:
            cursor = 0;
            clearHighlighting();
            break;
         case Keyboard.KEY_END:
            cursor = message.length();
            clearHighlighting();
            break;
         default:
            resetTabbing();
            // Verifies that the character is in the character set before adding
            if (updateCounter != 0) {
               if (CLOSE_WITH_OPEN_KEY && id == mod_Console.openKey.keyCode) {
                  mod_Console.closeConsole();
                  break;
               }
               if (ALLOWED_CHARACTERS.indexOf(key) >= 0 && this.message.length() < CHAT_INPUT_LENGTH_MAX && !(message.startsWith("/") && message.length() > CHAT_INPUT_LENGTH_SERVER_MAX - 1)) {
                  insertChar(key);
               }
            }
      }
   }
   
   private void insertChar (char key) {
      if (firstHighlighting[1] == lastHighlighting[1] || firstHighlighting[0] != -1 || lastHighlighting[1] != 1) {
         validateCursor();
         clearHighlighting();
         String start = message.substring(0, cursor);
         String end = message.substring(cursor, message.length());
         this.message = start + key + end;
         cursor++;
      } else {
         String start, end;
         if (firstHighlighting[1] < lastHighlighting[1]) {
            start = message.substring(0, firstHighlighting[1]);
            end = message.substring(lastHighlighting[1]);
         } else {
            start = message.substring(0, lastHighlighting[1]);
            end = message.substring(firstHighlighting[1]);
         }

         message = start + key + end;
         cursor = start.length() + 1;
         clearHighlighting();
      }
   }
   
   private int highlightMoveEnd (int diff) {
      //TODO fill out and rearrange code
      return 0;
   }
   
   private int highlightSetEnd (int line, int pos) {
      //TODO fill out and rearrange code
      return 0;
   }
   
   
   /**
    * Resets tabbing values an progress
    */
   private void resetTabbing()
   {
      tabbing = false;
      tabMatchPlayerNamesOnly = false;
      tabListPos = 0;
      tabWordPos = 0;
      tabMatchingWord = "";
      tabMatchedWord = "";
   }
   
   /**
    * Updates / starts the tabbing progress with appropriate offset
    * @param Diff is the offset value for the list
    */
   private void updateTabPos(int Diff)
   {
      if(tabbing == false)
      {
         //find the last word, defined via the cursor
         tabBeforeCursor = message.substring(0, cursor);
         tabAfterCursor = (cursor < message.length()) ? message.substring(cursor) : "" ;
         
         if (tabBeforeCursor == null || tabBeforeCursor.length() == 0) {
            tabMatchingWord = "";
            tabWordPos = 0;
         } else if (tabBeforeCursor.startsWith("@")){
            tabMatchingWord = tabBeforeCursor.substring(1);
            tabWordPos = 1;
            tabMatchPlayerNamesOnly = true;
         } else if (tabBeforeCursor.endsWith(" ")) {
            tabMatchingWord = "";
            tabWordPos = tabBeforeCursor.length();
         } else if (tabBeforeCursor.contains(" ")) {
            String [] splitMessage = tabBeforeCursor.split(" ");
            tabMatchingWord = splitMessage[splitMessage.length - 1];
            tabWordPos = tabBeforeCursor.length() - tabMatchingWord.length();
         } else {
            tabMatchingWord = tabBeforeCursor;
            tabWordPos = 0;
         }
      }
      
      List<String> autoWords;
      
      if(tabMatchPlayerNamesOnly)
         autoWords = getPlayerNames();       //list of player names only
      else
         autoWords = getAutoPossibility();   //list of all possible words
      
      if(autoWords == null)
         return;
      
      tabCurrentList = new ArrayList<String>(); 
      tabMaxPos = 0;
      if(tabMatchingWord == null){
         tabMaxPos = autoWords.size();
         tabCurrentList.addAll(autoWords);
      } else {
         for (int i = 0; i < autoWords.size(); i++) {
            String currentWord = autoWords.get(i);
            // Tests if a autoword starts with the matching word
            if (currentWord.toLowerCase().startsWith(tabMatchingWord.toLowerCase())) {
               tabCurrentList.add(currentWord);
            }
         }
         tabMaxPos = tabCurrentList.size();
      }

      if (tabCurrentList.size() > 0) {
         
         if(tabbing)
            tabListPos += Diff;
         else
            tabbing = true;
         
         //check for see if out of bound
         tabListPos = (tabListPos >= tabMaxPos)? 0 : tabListPos;
         tabListPos = (tabListPos < 0)? tabMaxPos - 1 : tabListPos;
         
         //tabListPos = (tabListPos > tabMaxPos)? tabMaxPos : tabListPos; //if player leaves whiles browsing words
         tabMatchedWord = tabCurrentList.get(tabListPos);
         
         if(message.length() > 0)
            message = message.substring(0, tabWordPos) + tabMatchedWord + tabAfterCursor;
         else
            message = tabMatchedWord;
         
         
         cursor = message.length();
      }
   }
   
   /**
    * Returns true if the current game is being player on a server
    *
    * @return True is returned when the current game is being played on a
    *         Minecraft server
    */
   public boolean isMultiplayerMode() {
      return !mc.isSingleplayer();
   }
   
   /**
    * Returns true/false depending on if the integrated server is running
    * 
    * @return True if the integrated server is running.
    */
   public boolean isLocalMultiplayerServer() {
      return mc.isIntegratedServerRunning();
   }
   
   /**
    * 
    * @link{ GuiConsole.cleanString }
    * @return cleaned servername or ""
    */
   public String getServerName() {
      if(isMultiplayerMode()) {
         String name = mc.getServerData().serverName;
         return (name.equals(null))? "" : cleanString(name);
      }
      return "";
   }
   
   public String getServerIp() {
      if(isMultiplayerMode())
         return mc.getServerData().serverIP;
      return "";
   }
   
   
   /**
    * Gets all the usernames on the current server you're on
    *
    * @return A list in alphabetical order of players logged onto the server
    */
   public List<String> getPlayerNames() {
      List<String> names = new ArrayList<String>();
      if (isMultiplayerMode() && mc.thePlayer instanceof EntityClientPlayerMP) {
         NetClientHandler netclienthandler = ((EntityClientPlayerMP) mc.thePlayer).sendQueue;
         List<GuiPlayerInfo> tempList = netclienthandler.playerInfoList;
         for (GuiPlayerInfo info : (List<GuiPlayerInfo>) tempList) {
            String name = info.name; //There were some problems with bukkit plugins adding prefixes or suffixes to the names list. This cleans the strings.
            Pattern pattern = Pattern.compile("[\\[[\\{[\\(]]]+?.*?[\\][\\}[\\)]]]"); //Get rid of everything between the brackets (), [], or {}
            Matcher matcher = pattern.matcher(name);
            name = matcher.replaceAll("");
            String cleanName = "";
            for (int i = 0; i < name.length(); i++) { //Get rid of every invalid character for minecraft usernames
               if (name.charAt(i) == '\u00a7') { //Gets rid of color codes
                  i++;
                  continue;
               }

               if (Character.isLetterOrDigit(name.charAt(i)) || name.charAt(i) == '_') {
                  cleanName += name.charAt(i);
               }
            }
            if (!cleanName.equals("")) {
               names.add(cleanName);
            }
         }
      } else {
         names.add(playername);
      }
      return names;
   }

   /**
    * Gets all possible autocomplete words, including playernames
    *
    * @return A list of all possible word completions for autoword
    */

   public List<String> getAutoPossibility()
   {
      List<String> autowords = new ArrayList<String>();
      List<String> players = getPlayerNames();
      if(players != null && players.size() > 0)
      {
         autowords.addAll(players);
      }

      autowords.addAll(ConsoleChatCommands.getChatCommands());

      return autowords;
   }

   /**
    * Changes the highlighting bounds so nothing is highlighted
    */

   public void clearHighlighting() {
      lastHighlighting[0] = -1;
      lastHighlighting[1] = 0;
      firstHighlighting[0] = -1;
      firstHighlighting[1] = 0;
   }

   /**
    * Cleans a dirty string of any invalid characters then returns the clean
    * string to the user. Verifies that the string doesn't go beyond the maximum
    * length as well
    *
    * @param dirty - The string to clean
    * @return A nice clean string
    */
   public static String cleanString(String dirty) {
      String clean = "";
      if (dirty == null) {
         return "";
      }
      char letters[] = dirty.toCharArray();
      for (char letter : letters) {
         if (ALLOWED_CHARACTERS.indexOf(letter) >= 0) {
            clean += letter;
         }
      }
      if (clean.length() >= CHAT_INPUT_LENGTH_MAX) {
         clean = clean.substring(0, CHAT_INPUT_LENGTH_MAX - 1);
      }
      return clean;
   }

   /**
    * Gets the String on the system clip board, if it exists. Otherwise null is
    * returned
    *
    * @return Returns the String on the clip board
    */
   public static String getClipboardString() {
      try {
         Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
         if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return cleanString((String) t.getTransferData(DataFlavor.stringFlavor));
         }
      } catch (Exception e) {
      }
      return null;
   }

   /**
    * Sets a String onto the system clip board.
    *
    * @param str - The string to copy onto the clip board
    */
   public static void setClipboardString(String str) {
      try {
         Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), null);
      } catch (Exception e) {
      }
   }
   
   public static String getInputPrefix() {
      return CHAT_INPUT_PREFIX;
   }
   
   /**
    * Command history implementation, sets the historyPosition pointer to
    * position based on its validity. The validity is verified by the method
    * and the pointer is kept between the bounds of the history
    *
    * @param position - The position to set the pointer to
    * @return The input at this position in history
    */
   private String getInputHistory(int position) {
      if (INPUT_HISTORY.size() == 0) {
         return "";
      }
      if (position <= 0) {
         position = 0;
         return "";
      }
      if (position > INPUT_HISTORY.size()) {
         position = INPUT_HISTORY.size();
      }
      historyPosition = position;
      return INPUT_HISTORY.elementAt(INPUT_HISTORY.size() - historyPosition);
   }

   /**
    * Validates that the cursor is in a valid position, if the cursor isn't
    * then the cursor is moved into the closest valid position.
    */
   private void validateCursor() {
      if (cursor > message.length()) {
         cursor = message.length();
      } else if (cursor < 0) {
         cursor = 0;
      }
   }

   /**
    * Sets the inputOffset value to the appropriate place
    */

   private void validateOffset() {
      String start = message.substring(0, cursor);
      String end = message.substring(cursor, message.length());
      input = CHAT_INPUT_PREFIX + start + ((updateCounter / 8) % 2 != 0 ? "." : "!") + end;

      if (fontRenderer.getStringWidth(input) >= TEXT_BOX[2] - TEXT_BOX[0] - SCREEN_BORDERSIZE * 2) {
         int upperbound = input.length();
         int boxsize = TEXT_BOX[2] - TEXT_BOX[0] - SCREEN_BORDERSIZE * 2;

         if (inputOffset < 0) {
            inputOffset = 0;
         }

         if (inputOffset > CHAT_INPUT_PREFIX.length()) {
            while (cursor < inputOffset - CHAT_INPUT_PREFIX.length() && inputOffset > 0) {
               inputOffset--;
            }
         } else {
            while (cursor < inputOffset && inputOffset > 0) {
               inputOffset--;
            }
         }

         while (fontRenderer.getStringWidth(input.substring(inputOffset, cursor + CHAT_INPUT_PREFIX.length() + 1)) >= boxsize) {
            inputOffset++;
         }

         while (fontRenderer.getStringWidth(input.substring(inputOffset, upperbound)) >= boxsize) {
            upperbound--;
         }

         if (upperbound > input.length()) {
            upperbound = input.length();
         }
         input = input.substring(inputOffset, upperbound);
      }
   }

   /**
    * Makes sure the highlighting values are within the string bounds
    */
   private void validateHighlighting() {
      if (firstHighlighting[0] < -1) {
         firstHighlighting[0] = -1;
      } else if (firstHighlighting[0] > LINES.size()) {
         firstHighlighting[0] = LINES.size();
      }

      if (lastHighlighting[0] < -1) {
         lastHighlighting[0] = -1;
      } else if (lastHighlighting[0] > LINES.size()) {
         lastHighlighting[0] = LINES.size();
      }

      if (firstHighlighting[0] == -1 && lastHighlighting[0] == -1) {
         if (lastHighlighting[1] < 0) {
            lastHighlighting[1] = 0;
         } else if (lastHighlighting[1] > message.length()) {
            lastHighlighting[1] = message.length();
         }

         if (firstHighlighting[1] < 0) {
            firstHighlighting[1] = 0;
         } else if (firstHighlighting[1] > message.length()) {
            firstHighlighting[1] = message.length();
         }
      } else {
         if (lastHighlighting[1] < 0) {
            lastHighlighting[1] = 0;
         } else if (lastHighlighting[1] > LINES.get(lastHighlighting[0]).length()) {
            lastHighlighting[1] = LINES.get(lastHighlighting[0]).length();
         }

         if (firstHighlighting[1] < 0) {
            firstHighlighting[1] = 0;
         } else if (firstHighlighting[1] > LINES.get(firstHighlighting[0]).length()) {
            firstHighlighting[1] = LINES.get(firstHighlighting[0]).length();
         }
      }
   }

   /**
    * Paste clipboard at cursor position.
    */
   private void paste() {
      String clipboard = getClipboardString();
      if (clipboard != null) {
         if (lastHighlighting[1] == firstHighlighting[1] || firstHighlighting[0] != -1 || lastHighlighting[0] != -1) {
            String start = "";
            String end = "";
            validateCursor();

            if (message != null && message.length() > 0) {
               start = message.substring(0, cursor);
               end = message.substring(cursor);
            }

            int limit = CHAT_INPUT_LENGTH_MAX - message.length();
            if (limit < clipboard.length()) {
               clipboard = clipboard.substring(0, limit);
            }

            message = start + clipboard + end;
            cursor = (start + clipboard).length() + 1;
         } else {
            String start, end;
            if (firstHighlighting[1] < lastHighlighting[1]) {
               start = message.substring(0, firstHighlighting[1]);
               end = message.substring(lastHighlighting[1]);
            } else {
               start = message.substring(0, lastHighlighting[1]);
               end = message.substring(firstHighlighting[1]);
            }

            int limit = CHAT_INPUT_LENGTH_MAX - message.length();
            if (limit < clipboard.length()) {
               clipboard = clipboard.substring(0, limit);
            }

            message = start + clipboard + end;
            cursor = (start + clipboard).length() + 1;
            clearHighlighting();
         }
      }
   }

   /**
    * Delete the next character.
    */
   private void delete() {
      // Delete
      if (message.length() > 0) {
         if (firstHighlighting[1] == lastHighlighting[1] || firstHighlighting[0] != -1 || lastHighlighting[0] != -1) {
            validateCursor();
            String start = message.substring(0, cursor);
            String end = message.substring(cursor, message.length());
            this.message = start + (end.length() > 0 ? end.substring(1) : end);
         } else if (firstHighlighting[0] == -1 && lastHighlighting[0] == -1) {
            String start, end;
            if (firstHighlighting[1] < lastHighlighting[1]) {
               start = message.substring(0, firstHighlighting[1]);
               end = message.substring(lastHighlighting[1]);
            } else {
               start = message.substring(0, lastHighlighting[1]);
               end = message.substring(firstHighlighting[1]);
            }
            message = start + end;
         }
         clearHighlighting();
      }
   }

   /**
    * Called per frame to draw the new frame
    *
    * @see net.minecraft.src.GuiScreen#drawScreen(int, int, float)
    */
   @Override
   public void drawScreen(int mousex, int mousey, float f) {
      // Background
      int minx = SCREEN_PADDING_LEFT;
      int miny = SCREEN_PADDING_TOP;
      int maxx = width - SCREEN_PADDING_RIGHT;
      int maxy = height - SCREEN_PADDING_BOTTOM;
      drawRect(minx, miny, maxx, maxy, COLOR_BASE);
      
      // Input Text box
      int textbox_minx = minx + SCREEN_BORDERSIZE;
      int textbox_maxx = maxx - SCREEN_BORDERSIZE;
      int textbox_miny = maxy - CHARHEIGHT - SCREEN_BORDERSIZE;
      int textbox_maxy = maxy - SCREEN_BORDERSIZE;
      
      if(SCREEN_MESSAGE_LENGHT_DISPLAY) {
         
         String currentChars = String.valueOf(message.length());
         String maxLenght = String.valueOf(CHAT_INPUT_LENGTH_SERVER_MAX);
         int charDiff = maxLenght.length() - currentChars.length();
         for (int i = 0; i < charDiff; i++) {
            currentChars = "0" + currentChars;
         }
         
         String messageLenght = currentChars + "/" + maxLenght;
         
         int indent = mc.fontRenderer.getStringWidth(messageLenght) + SCREEN_BORDERSIZE*2;
         
         int messageLenghtBox_maxx = textbox_maxx;
         int messageLenghtBox_minx = textbox_maxx - indent + SCREEN_BORDERSIZE;
         
         
         drawRect(messageLenghtBox_minx, textbox_miny, messageLenghtBox_maxx, textbox_maxy, COLOR_MESSAGE_LENGTH_BACKGROUND);
         
         drawRect(textbox_minx, textbox_miny, textbox_maxx - indent, textbox_maxy, COLOR_INPUT_BACKGROUND);
         TEXT_BOX = new int[] { textbox_minx, textbox_miny, textbox_maxx - indent, textbox_maxy };
         drawString(fontRenderer, messageLenght, messageLenghtBox_minx + 1, textbox_miny + 1, COLOR_INPUT_TEXT);
         
      } else {
         drawRect(textbox_minx, textbox_miny, textbox_maxx, textbox_maxy, COLOR_INPUT_BACKGROUND);
         TEXT_BOX = new int[] { textbox_minx, textbox_miny, textbox_maxx, textbox_maxy };
      }
      
      
      

      // Input text highlighting
      if (firstHighlighting[0] == -1 && lastHighlighting[0] == -1 && lastHighlighting[1] != firstHighlighting[1]) {
         int firstH, lastH; //First letter position, last letter position
         if (firstHighlighting[1] < lastHighlighting[1]) {
            firstH = firstHighlighting[1];
            lastH = lastHighlighting[1];
         } else {
            firstH = lastHighlighting[1];
            lastH = firstHighlighting[1];
         }

         if (firstH < 0) {
            firstH = 0;
         }

         if (lastH > message.length()) {
            lastH = message.length();
         }

         String messageSection;
         validateCursor();
         validateOffset();
         validateHighlighting();
         if (inputOffset < CHAT_INPUT_PREFIX.length()) {
            messageSection = (CHAT_INPUT_PREFIX + message.substring(0, firstH)).substring(inputOffset);
         } else {
            messageSection = message.substring((inputOffset - CHAT_INPUT_PREFIX.length()) > firstH ? firstH : inputOffset - CHAT_INPUT_PREFIX.length(), firstH);
         }

         int highlighting_minx = 1 + TEXT_BOX[0] + fontRenderer.getStringWidth(messageSection);

         if (firstH < inputOffset - CHAT_INPUT_PREFIX.length()) {
            messageSection = message.substring(inputOffset - CHAT_INPUT_PREFIX.length(), lastH);
         } else {
            messageSection = message.substring(firstH, lastH);
         }

         int highlighting_maxx = 1 + highlighting_minx + fontRenderer.getStringWidth(messageSection);
         int highlighting_miny = TEXT_BOX[1];
         int highlighting_maxy = highlighting_miny + CHARHEIGHT;
         int ExclamationStringWidth = fontRenderer.getStringWidth("!");
         
         if (cursor > firstH && cursor < lastH)
            highlighting_maxx += ExclamationStringWidth;
         else if (cursor <= firstH) {
            highlighting_minx += ExclamationStringWidth;
            highlighting_maxx += ExclamationStringWidth;
         }

         if (highlighting_maxx > TEXT_BOX[2]) {
            highlighting_maxx = TEXT_BOX[2];
         }

         drawRect(highlighting_minx, highlighting_miny, highlighting_maxx, highlighting_maxy, COLOR_TEXT_HIGHLIGHT);
      }

      // Past messages - dialog
      int message_miny = miny + SCREEN_BORDERSIZE;
      int message_maxy = textbox_miny - SCREEN_BORDERSIZE;
      int chatTemp = maxx - (SCREEN_BORDERSIZE * 2) - 10 - textbox_minx;
      if (currentChatWidth != chatTemp) {
         currentChatWidth = chatTemp;
         buildLines();
      }

      HISTORY = new int[] { textbox_minx, message_miny, textbox_minx + currentChatWidth, message_maxy };

      if (LINES == null || rebuildLines) {
         buildLines();
      }

      drawRect(textbox_minx, message_miny, textbox_minx + currentChatWidth, message_maxy, COLOR_OUTPUT_BACKGROUND);

      // Past messages - highlighting

      if (firstHighlighting[0] > -1 && lastHighlighting[0] > -1 && !Arrays.equals(firstHighlighting, lastHighlighting)) {
         int maxDisplayedLines = (HISTORY[3] - HISTORY[1]) / (CHARHEIGHT - 1);
         int linesDisplayed = LINES.size() >= maxDisplayedLines ? maxDisplayedLines : LINES.size();
         int lineAtOnScreen_i;
         if (linesDisplayed < maxDisplayedLines) {
            lineAtOnScreen_i = maxDisplayedLines - LINES.size() + firstHighlighting[0];
         } else {
            lineAtOnScreen_i = firstHighlighting[0] - LINES.size() + linesDisplayed + slider;
         }

         int lineAtOnScreen_f;
         if (linesDisplayed < maxDisplayedLines) {
            lineAtOnScreen_f = maxDisplayedLines - LINES.size() + lastHighlighting[0];
         } else {
            lineAtOnScreen_f = lastHighlighting[0] - LINES.size() + linesDisplayed + slider;
         }

         int[] rect = new int[4];
         int xoffset = HISTORY[0] + SCREEN_BORDERSIZE;
         int yoffset = HISTORY[1] + SCREEN_BORDERSIZE;

         //initial
         int h_minx = xoffset + fontRenderer.getStringWidth(LINES.get(firstHighlighting[0]).substring(0, firstHighlighting[1])) - 1;
         int h_miny = yoffset + ((CHARHEIGHT - 1) * lineAtOnScreen_i) - 2;
         int h_maxx = h_minx + fontRenderer.getStringWidth(LINES.get(firstHighlighting[0]).substring(firstHighlighting[1])) + 2;
         int h_maxy = h_miny + CHARHEIGHT;

         if (lastHighlighting[0] != firstHighlighting[0]) {
            if (lastHighlighting[0] < firstHighlighting[0]) {
               h_maxx = xoffset - 1;
            }

            drawRect(h_minx, h_miny, h_maxx, h_maxy, COLOR_TEXT_HIGHLIGHT);

            //inbetween
            int firstOnScreen = lineAtOnScreen_i <= lineAtOnScreen_f ? lineAtOnScreen_i : lineAtOnScreen_f;
            int firstInLINES = firstHighlighting[0] <= lastHighlighting[0] ? firstHighlighting[0] : lastHighlighting[0];

            for (int i = 1; i < Math.abs(lineAtOnScreen_i - lineAtOnScreen_f); i++) {
               h_minx = xoffset - 1;
               h_miny = yoffset + ((CHARHEIGHT - 1) * (firstOnScreen + i)) - 2;
               h_maxx = h_minx + fontRenderer.getStringWidth(LINES.get(firstInLINES + i)) + 2;
               h_maxy = h_miny + CHARHEIGHT;

               drawRect(h_minx, h_miny, h_maxx, h_maxy, COLOR_TEXT_HIGHLIGHT);
            }

            //final
            h_minx = xoffset + fontRenderer.getStringWidth(LINES.get(lastHighlighting[0]).substring(0, lastHighlighting[1])) - 1;
            h_miny = yoffset + ((CHARHEIGHT - 1) * lineAtOnScreen_f) - 2;
            h_maxx = h_minx + fontRenderer.getStringWidth(LINES.get(lastHighlighting[0]).substring(lastHighlighting[1])) + 2;
            h_maxy = h_miny + CHARHEIGHT;

            if (lastHighlighting[0] > firstHighlighting[0]) {
               h_maxx = xoffset - 1;
            }

            drawRect(h_minx, h_miny, h_maxx, h_maxy, COLOR_TEXT_HIGHLIGHT);
         } else {
            h_maxx = xoffset + fontRenderer.getStringWidth(LINES.get(lastHighlighting[0]).substring(0, lastHighlighting[1])) - 1;
            drawRect(h_minx, h_miny, h_maxx, h_maxy, COLOR_TEXT_HIGHLIGHT);
         }
      }

      // Past messages - text
      int max = (message_maxy - message_miny) / (CHARHEIGHT - 1);
      if (slider != 0) {
         slider = LINES.size() - slider > max ? (LINES.size() - slider < LINES.size() ? slider : 0) : LINES.size() - max;
      }

      if (slider < 0)
         slider = 0;

      int oversize = 0;
      for (int i = 0; i + oversize < LINES.size() && i + oversize < max; i++) {
         int element = LINES.size() - 1 - i - slider;
         if (LINES.size() <= element)
            continue;
         drawString(this.mc.fontRenderer, LINES.elementAt(element), textbox_minx + SCREEN_BORDERSIZE, textbox_miny - CHARHEIGHT + 1 - SCREEN_BORDERSIZE - ((i + oversize) * (CHARHEIGHT - 1)), COLOR_TEXT_OUTPUT);
      }

      // Scroll - background
      int scroll_minx = textbox_minx + currentChatWidth + SCREEN_BORDERSIZE;
      int scroll_maxx = textbox_maxx;
      int scroll_miny = message_miny;
      int scroll_maxy = textbox_miny - SCREEN_BORDERSIZE;
      drawRect(scroll_minx, scroll_miny, scroll_maxx, scroll_maxy, COLOR_SCROLL_BACKGROUND);

      // Scroll - button top
      TOP = new int[] { scroll_minx + 1, scroll_miny + 1, scroll_maxx - 1, scroll_miny + 9 };
      drawRect(TOP[0], TOP[1], TOP[2], TOP[3], COLOR_SCROLL_FOREGROUND);
      drawString(this.mc.fontRenderer, "^", TOP[0] + 2, TOP[1] + 2, COLOR_SCROLL_ARROW);

      // Scroll - button bottom
      BOTTOM = new int[] { scroll_minx + 1, scroll_maxy - 9, scroll_maxx - 1, scroll_maxy - 1 };
      drawRect(BOTTOM[0], BOTTOM[1], BOTTOM[2], BOTTOM[3], COLOR_SCROLL_FOREGROUND);
      drawStringFlipped(this.mc.fontRenderer, "^", BOTTOM[0] + 1, BOTTOM[1] - 3, COLOR_SCROLL_ARROW, true);

      // Scroll - bar
      int scrollable_minx = scroll_minx + 1;
      int scrollable_maxx = scroll_maxx - 1;
      int scrollable_miny = scroll_miny + 11;
      int scrollable_maxy = scroll_maxy - 10;
      sliderHeight = scrollable_maxy - scrollable_miny;
      double heightpercentage = (double) max / (double) LINES.size();
      double barheight = (sliderHeight) * heightpercentage;
      barheight = (barheight < 5) ? 5 : barheight;
      double stepsize = (sliderHeight - barheight) / (double) (LINES.size() - max);
      double position = slider * stepsize;

      if (LINES.size() < max)
         BAR = new int[] { scrollable_minx, scrollable_miny, scrollable_maxx, scrollable_maxy };
      else
         BAR = new int[] { scrollable_minx, (int) (scrollable_maxy - position - barheight), scrollable_maxx, (int) (scrollable_maxy - position) };
      drawRect(BAR[0], BAR[1], BAR[2], BAR[3], COLOR_SCROLL_FOREGROUND);

      // Input
      validateCursor();
      validateOffset();
      drawString(this.mc.fontRenderer, input, textbox_minx + SCREEN_BORDERSIZE, textbox_miny + 1, COLOR_INPUT_TEXT);
      
      //autocomplete wordmatch visual
      int linesToShow = (int) Math.floor(SCREEN_PADDING_BOTTOM / CHARHEIGHT) - 1;
      
      if(tabbing && SCREEN_AUTOPREVIEW && linesToShow > 0)
      {
         int tabStartPos = fontRenderer.getStringWidth(CHAT_INPUT_PREFIX + " " + message.substring(0, tabWordPos));
         if(tabStartPos + SCREEN_AUTOPREVIEWAREA > width)
            tabStartPos = width - SCREEN_AUTOPREVIEWAREA;
         
         drawRect(textbox_minx - SCREEN_BORDERSIZE*2 + tabStartPos, textbox_maxy + SCREEN_BORDERSIZE, tabStartPos + SCREEN_AUTOPREVIEWAREA, height, COLOR_BASE);
         
         String currentPos = String.valueOf(tabListPos + 1);
         String endPos = String.valueOf(tabMaxPos);
         int charDiff = endPos.length() - currentPos.length();
         for (int i = 0; i < charDiff; i++) {
            currentPos = "0" + currentPos;
         }
         
         String positionText = "[" + currentPos + "/" + endPos + "]";
      
         for (int i = 0; i < linesToShow; i++) {
            drawString(this.mc.fontRenderer, tabCurrentList.get((tabListPos + i + 1)%(tabMaxPos)), textbox_minx + tabStartPos - SCREEN_BORDERSIZE, textbox_maxy + SCREEN_BORDERSIZE + i*CHARHEIGHT, COLOR_INPUT_TEXT);
         }
         
         drawString(this.mc.fontRenderer, positionText, textbox_minx + tabStartPos - SCREEN_BORDERSIZE, textbox_maxy + SCREEN_BORDERSIZE + linesToShow*CHARHEIGHT, COLOR_INPUT_TEXT);
      }
      
      // Titlebar
      drawRect(maxx / 2, 0, maxx, miny, COLOR_BASE);

      // Title
      drawString(this.mc.fontRenderer, TITLE, (maxx / 2) + SCREEN_BORDERSIZE, SCREEN_BORDERSIZE, COLOR_TEXT_TITLE);
      
      // Options button 
      if(mod_Console.GuiApiInstalled()) {
         OPTION_BUTTON = new int[] { maxx - SCREEN_BORDERSIZE*3 - 30, SCREEN_BORDERSIZE, maxx - SCREEN_BORDERSIZE*3 - 20, miny };
         drawRect( OPTION_BUTTON[0], OPTION_BUTTON[1], OPTION_BUTTON[2], OPTION_BUTTON[3], COLOR_EXIT_BUTTON );
         drawString(this.mc.fontRenderer, "|:.", maxx - SCREEN_BORDERSIZE*3 - 28, SCREEN_BORDERSIZE + 2, COLOR_EXIT_BUTTON_TEXT);
      }
      
      // External window button 
      EXTERNAL_BUTTON = new int[] { maxx - SCREEN_BORDERSIZE*2 - 20, SCREEN_BORDERSIZE, maxx - SCREEN_BORDERSIZE*2 - 10, miny };
      drawRect( EXTERNAL_BUTTON[0], EXTERNAL_BUTTON[1], EXTERNAL_BUTTON[2], EXTERNAL_BUTTON[3], COLOR_EXIT_BUTTON );
      drawString(this.mc.fontRenderer, "[]", maxx - SCREEN_BORDERSIZE*2 - 19, SCREEN_BORDERSIZE + 2, COLOR_EXIT_BUTTON_TEXT);
      
      // Exit button
      EXIT_BUTTON = new int[] { maxx - SCREEN_BORDERSIZE - 10, SCREEN_BORDERSIZE, maxx - SCREEN_BORDERSIZE, miny };
      drawRect( EXIT_BUTTON[0], EXIT_BUTTON[1], EXIT_BUTTON[2], EXIT_BUTTON[3], COLOR_EXIT_BUTTON );
      drawString(this.mc.fontRenderer, "X", maxx - SCREEN_BORDERSIZE - 7, SCREEN_BORDERSIZE + 2, COLOR_EXIT_BUTTON_TEXT);
      

      super.drawScreen(mousex, mousey, f);
   }

   /**
    * Draws the specified String flipped upside down
    *
    * @param fontrenderer
    * @param s string to draw
    * @param i position of the x coordinate
    * @param j position of the y coordinate
    * @param k colour of the render
    * @param flag if true draw with shadow, if false draw without shadow
    */
   
   public void drawStringFlipped(FontRenderer fontrenderer, String s, int i, int j, int k, boolean flag) {
      GL11.glPushMatrix();
      GL11.glScalef(-1F, -1F, 1F);
      GL11.glTranslatef((-i * 2) - fontrenderer.getStringWidth(s), (-j * 2) - fontrenderer.FONT_HEIGHT, 0.0F);
      if (flag) {
         fontrenderer.drawString(s, i - 1, j - 1, (k & 0xfcfcfc) >> 2 | k & 0xff000000); //Took the last argument from FrontRenderer.renderString() because it's private and I want the shadow on the correct side when flipped
      }
      fontrenderer.drawString(s, i, j, k);
      GL11.glPopMatrix();
   }

   /**
    * Tests whether the (x, y) coordinate is within the rectangle or not
    *
    * @param x x coordinate
    * @param y y coordinate
    * @param rect integer array in the form of {mix x, min y, max x, max y}
    * @return true if point is in rect false if point is not in rect
    */

   public boolean hitTest(int x, int y, int[] rect) {
      if (x >= rect[0] && x <= rect[2] && y >= rect[1] && y <= rect[3])
         return true;
      else
         return false;
   }

   /**
    * Returns the mouse position as an index within the string line
    *
    * @param x The mouse's x position relative to the start of the string line
    * @param line The string to find the index in
    * @return the character index the mouse clicked at.
    */

   public int mouseAt(int x, String line) {
      int left = 0;
      int right = line.length();

      if (x >= fontRenderer.getStringWidth(line)) {
         return line.length();
      }

      while (left <= right) {
         int middle = (left + right) / 2;
         int length = fontRenderer.getStringWidth(line.substring(0, middle));
         double upper, lower;
         if (middle < line.length() - 1) {
            upper = length + (fontRenderer.getStringWidth(Character.toString(line.charAt(middle))) / 2.0);
         } else {
            upper = fontRenderer.getStringWidth(line);
         }

         if (middle >= 1) {
            lower = length - (fontRenderer.getStringWidth(Character.toString(line.charAt(middle - 1))) / 2.0);
         } else {
            lower = 0;
         }

         if ((x <= upper && x >= lower)) {
            return middle;
         } else if (x < lower) {
            right = middle - 1;
         } else if (x > upper) {
            left = middle + 1;
         }
      }

      return 0;
   }

   /**
    * Called on mouse clicked and processes the button clicks and actions
    *
    * @see net.minecraft.src.GuiScreen#mouseClicked(int, int, int)
    */
   @Override
   protected void mouseClicked(int mousex, int mousey, int button) {
      if (button == 0) {
         // Bad implementation which checks for clicks on exit button
         if (hitTest(mousex, mousey, EXIT_BUTTON)) {
            mc.displayGuiScreen(null);
            resetTabbing();
            return;
         } else if (hitTest(mousex, mousey, EXTERNAL_BUTTON)) {
            ExternalGuiConsole.toggleExternalWIndow();
         } else if (mod_Console.GuiApiInstalled() && hitTest(mousex, mousey, OPTION_BUTTON)){
            mc.displayGuiScreen(null);
            GuiModScreen.show(new GuiModScreen(INSTANCE, ConsoleSettings.getMainWindow()));
            resetTabbing();
            return;
         // Bad implementation which checks for clicks on scrollbar
         }else if (hitTest(mousex, mousey, TOP))
            slider++;
         else if (hitTest(mousex, mousey, BOTTOM))
            slider--;
         else if (hitTest(mousex, mousey, BAR)) {
            isSliding = true;
            lastSliding = mousey;
            initialSliding = slider;
         } else if (hitTest(mousex, mousey, TEXT_BOX)) {
            resetTabbing();
            isHighlighting = true;
            firstHighlighting[0] = lastHighlighting[0] = -1;
            int mousexCorrected = mousex - TEXT_BOX[0] - SCREEN_BORDERSIZE;
            int startStringIndex = 0;
            int cutPrefixChars = (inputOffset <= CHAT_INPUT_PREFIX.length() ? inputOffset : CHAT_INPUT_PREFIX.length());
            if (inputOffset < CHAT_INPUT_PREFIX.length()) {
               mousexCorrected -= fontRenderer.getStringWidth(CHAT_INPUT_PREFIX.substring(inputOffset));
            } else {
               startStringIndex = inputOffset - CHAT_INPUT_PREFIX.length();
            }

            if (mousexCorrected > fontRenderer.getStringWidth(message.substring(startStringIndex, cursor) + "!")) {
               mousexCorrected -= fontRenderer.getStringWidth("!");
            }

            int charat = mouseAt(mousexCorrected, message.substring(startStringIndex)) + startStringIndex;
            if (message.length() < charat)
               firstHighlighting[1] = message.length();
            else
               firstHighlighting[1] = charat;
            cursor = lastHighlighting[1] = firstHighlighting[1];
         } else if (hitTest(mousex, mousey, HISTORY)) {
            resetTabbing();
            isHighlighting = true;
            int mousexCorrected = mousex - HISTORY[0] - SCREEN_BORDERSIZE;
            int lineAt = correctYlineAt(mousey);
            firstHighlighting[0] = lineAt;
            int charAt = mouseAt(mousexCorrected, LINES.get(lineAt));
            firstHighlighting[1] = charAt;
         }

         super.mouseClicked(mousex, mousey, button);
      }
   }

   /**
    * Method is called on mouse movement and used to determine slider movement
    *
    * @see net.minecraft.src.GuiScreen#mouseMovedOrUp(int, int, int)
    */
   @Override
   protected void mouseMovedOrUp(int mousex, int mousey, int button) {
      int wheel = Mouse.getDWheel();
      if (wheel != 0) {
         slider += wheel / 120 * SCREEN_LINES_PER_SCROLL;
      }

      // Moves the slider position
      if (isSliding) {
         if (Mouse.isButtonDown(0)) {
            int diff = lastSliding - mousey;
            if (diff != 0) {
               slider = initialSliding + (int) ((diff / (double) sliderHeight) * (LINES.size()));
            }
         } else {
            isSliding = false;
            lastSliding = 0;
            initialSliding = 0;
         }
      } else if (isHighlighting) {
         if (hitTest(mousex, mousey, TEXT_BOX) || firstHighlighting[0] == -1) {
            int mousexCorrected = mousex - TEXT_BOX[0] - SCREEN_BORDERSIZE;
            int startStringIndex = 0;

            if (inputOffset < CHAT_INPUT_PREFIX.length()) {
               mousexCorrected -= fontRenderer.getStringWidth(CHAT_INPUT_PREFIX.substring(inputOffset));
            } else {
               startStringIndex = inputOffset - CHAT_INPUT_PREFIX.length();
            }

            if (mousexCorrected > fontRenderer.getStringWidth(message.substring(startStringIndex, cursor) + "!")) {
               mousexCorrected -= fontRenderer.getStringWidth("!");
            }

            int charat = mouseAt(mousexCorrected, message.substring(startStringIndex)) + startStringIndex;
            if (charat < 0) {
               charat = 0;
            }
            if (message.length() < charat) {
               lastHighlighting[1] = message.length();
            } else {
               lastHighlighting[1] = charat;
            }
            if (firstHighlighting[0] == lastHighlighting[0] && firstHighlighting[0] == -1) {
               cursor = lastHighlighting[1];
               validateCursor();
            }
            validateOffset();
         } else if (hitTest(mousex, mousey, HISTORY)) {
            resetTabbing();
            isHighlighting = true;
            int mousexCorrected = mousex - HISTORY[0] - SCREEN_BORDERSIZE;
            int lineAt = correctYlineAt(mousey);
            lastHighlighting[0] = lineAt;
            lineAt = (lineAt >= LINES.size())? LINES.size() - 1: lineAt; 
            int charAt = mouseAt(mousexCorrected, LINES.get(lineAt));
            lastHighlighting[1] = charAt;
         }
         if (!Mouse.isButtonDown(0)) {
            isHighlighting = false;
         }
      }
   }

   private int correctYlineAt(int mousey)
   {
      int maxDisplayedLines = (HISTORY[3] - HISTORY[1]) / (CHARHEIGHT - 1);
      int linesDisplayed = LINES.size() >= maxDisplayedLines ? maxDisplayedLines : LINES.size();
      int mouseyCorrected = mousey - HISTORY[1] - SCREEN_BORDERSIZE;
      int lineAt = mouseyCorrected / (CHARHEIGHT - 1) + LINES.size() - linesDisplayed - slider;

      if (lineAt >= LINES.size()) {
         lineAt = LINES.size() - maxDisplayedLines + lineAt;
         if (lineAt < 0) {
            lineAt = 0;
         }
         else if (lineAt >= LINES.size()) {
            lineAt = LINES.size() - 1;
         }
      }
      
      return lineAt;
   }

   /**
    * Returns true if the GUI is open
    *
    * @return Returns whether the GUI is open or not
    */
   public boolean isGuiOpen() {
      return isGuiOpen;
   }

   /**
    * Adds a console listener to the console. When events are triggered they
    * are then sent to all the listeners in the order which they are registered
    * in
    *
    * @param cl - The listener to add
    */
   public void addConsoleListener(ConsoleListener cl) {
      if (!LISTENERS.contains(cl)) {
         LISTENERS.add(cl);
      }
   }

   /**
    * Adds an input message to the console
    *
    * @param message - The input message
    */
   private void addInputMessage(String innMessage) {
      String message = innMessage;
      
      if (CHAT_PRINT_INPUT) {
         MESSAGES.add(CHAT_INPUT_PREFIX + message);
         addLine(CHAT_INPUT_PREFIX + message);
      }
      
      INPUT_HISTORY.add(message);
      if ((LOGGING & LOGGING_INPUT) > 0) {
         log.add(CHAT_INPUT_PREFIX + message);
      }

      boolean post = true;
      for (ConsoleListener cl : LISTENERS) {
         if (!cl.processInput(message)) {
            post = false;
         }
      }

      if (post) {
         int lastLen = 0;
         for (int i = 0; i <= message.length() / CHAT_INPUT_LENGTH_SERVER_MAX; i++) {
            int end = (lastLen + CHAT_INPUT_LENGTH_SERVER_MAX) > message.length() ? message.length() : (lastLen + CHAT_INPUT_LENGTH_SERVER_MAX);
            if (message.length() > CHAT_INPUT_LENGTH_SERVER_MAX && message.substring(lastLen, end).length() >= CHAT_INPUT_LENGTH_SERVER_MAX) {
               for (int j = 1; j <= 10; j++) {
                  if (message.charAt(end - j) == ' ') { //Wrap at space if it's within 10 characters
                     end = end - j;
                     break;
                  }
               }
            }

            mc.thePlayer.sendChatMessage(message.substring(lastLen, end));

            if (message.length() == CHAT_INPUT_LENGTH_SERVER_MAX) {
               break; //Fix for displaying an extra line when length is exactly at the limit
            }
            lastLen = end;
         }
      }
      
      if(CHAT_UNPASUE_PAUSE_WITH_MESSAGE && isGuiOpen) {
         pauseGame = false;
         pauseCountDown = 2;
      }
   }
   
   /**
    * Handles unclean messages from other
    * 
    * @param message
    */
   public void sendUncleanMessage(String message) {
      String cleanMessage = cleanString(message);
      if (!cleanMessage.isEmpty() && VALID_MESSAGE.matcher(cleanMessage).find()) {
         addInputMessage(cleanMessage);
      }
   }
   
   
   /**
    * Handles messages received as client 
    * 
    * @param message - the message
    */
   public void addClientMessage(String message) {
      addOutputMessage(message);
   }
   
   
   /**
    * Handles message sent as server
    * 
    * @param handler - who sent the message
    * @param message - the message
    */
   public void addServerMessage(NetServerHandler handler, String message) {
      if(!handler.getPlayer().username.equals(playername))
         addOutputMessage(CHAT_INPUT_PREFIX + message);
   }
   
   
   /**
    * Adds an output message to the console
    *
    * @param message - The output message
    */
   public void addOutputMessage(String message) {
      if (CHAT_PRINT_OUTPUT) {
         MESSAGES.add(message);
         addLine(message);
      }
      if ((LOGGING & LOGGING_OUTPUT) > 0) {
         log.add(message);
      }

      for (ConsoleListener cl : LISTENERS) {
         cl.processOutput(message);
      }
      
   }

   /**
    * Not anymore, new default at 100ms.
    * (Runs a thread which automatically pulls the chat line into the console
    * on a configurable interval, by default 20ms. It uses Object.equals
    * against the ChatLine object to determine the previous message which was
    * copied across.)
    *
    * This method also clears the history and output lists once they reach
    * capacity
    *
    * This method also handles key bindings
    *
    * @see java.lang.Runnable#run()
    */
   @Override
   public void run() {
      while (true) {
         try {
            if (logName == null) {
               logName = (new SimpleDateFormat(DATE_FORMAT_FILENAME)).format(new Date());
            }
            if (lastWrite + LOG_WRITE_INTERVAL < System.currentTimeMillis()) {
               try {
                  FileOutputStream fos = new FileOutputStream(new File(LOG_DIR, logName), true);
                  while (log.size() > 0) {
                     String line = sdf.format(new Date()) + log.elementAt(0) + LINE_BREAK;
                     fos.write(line.getBytes());
                     log.remove(0);
                  }
                  fos.close();
                  lastWrite = System.currentTimeMillis();
               } catch (FileNotFoundException e) {
               }
            }

            // Empties message list when it hits maximum size
            while (CHAT_OUTPUT_MAX != 0 && MESSAGES.size() > CHAT_OUTPUT_MAX) {
               MESSAGES.remove(0);
               rebuildLines = true;
            }

            // Empties input history list when it hits maximum size
            while (INPUT_HISTORY.size() > CHAT_INPUT_HISTORY_MAX) {
               INPUT_HISTORY.remove(0);
            }

            // Verify key down list items are still down - repeat event workaround
            for (int i = 0; i < keyDown.size(); i++) {
               if (!Keyboard.isKeyDown(keyDown.get(i))) {
                  keyDown.remove(keyDown.get(i));
               }
            }

            // Key bindings
            // String <code,code,code,...> : value > HashMap<String(codes),String(value)>
            Iterator<String> i = keyBindings.keySet().iterator();
            while (i.hasNext()) {
               String k = i.next();
               if (k == null) {
                  continue;
               }
               String keys[] = k.split(",");
               try {
                  boolean execute = true;
                  int keydown = 0;
                  for (String key : keys) {
                     int keyvalue = Integer.parseInt(key);
                     if (!Keyboard.isKeyDown(keyvalue)) {
                        execute = false;
                        break;
                     }
                     if (keyDown.contains(keyvalue)) {
                        keydown++;
                     }
                     keyDown.add(keyvalue);
                  }
                  if (execute && keydown < keys.length) {
                     if (BACKGROUND_BINDING_EVENTS || mc.currentScreen == null) {
                        // Adds binding message to input: addInputMessage(keyBindings.get(k));
                        mc.thePlayer.sendChatMessage(keyBindings.get(k));
                     }
                  }
               } catch (Exception e) { // If the number can't parse it is invalid anyway
               }
            }

            Thread.sleep(POLL_DELAY);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Reads the settings from the specified file and sets them to their
    * applicable variables
    *
    * @param base - The class to set the settings in
    * @param settings - The settings file to load the values from
    */
   public static void readSettings(Class<?> base, File settings) {
      Properties p = new Properties();
      try {
         File CanonicalFile = settings.getCanonicalFile();
         p.load(new FileInputStream(CanonicalFile));
         Field[] declaredFields = base.getDeclaredFields();
         for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
               try {
                  if (!field.isAccessible()) {
                     field.setAccessible(true);
                  }

                  if (field.getType().equals(String.class)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, property);
                     }
                  } else if (field.getType().equals(Integer.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Integer.decode(property).intValue());
                     }
                  } else if (field.getType().equals(Double.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Double.parseDouble(property));
                     }
                  } else if (field.getType().equals(Boolean.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Boolean.parseBoolean(property));
                     }
                  } else if (field.getType().equals(Long.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Long.decode(property).longValue());
                     }
                  } else if (field.getType().equals(Byte.TYPE)) { // new
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Byte.parseByte(property));
                     }
                  } else if (field.getType().equals(Float.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Float.parseFloat(property));
                     }
                  } else if (field.getType().equals(Short.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Short.decode(property));
                     }
                  } else if (field.getType().equals(Character.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, property.charAt(0));
                     }
                  }
               } catch (Exception e) {
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Writes the variables from the specified class into the specified file
    * in a .properties format
    *
    * @param base - The class to get the settings from
    * @param settings - The settings file to save the values to
    */
   public static void writeSettings(Class<?> base, File settings) {
      Properties p = new Properties();
      try {
         File CanonicalFile = settings.getCanonicalFile();
         try {
            p.load(new FileInputStream(CanonicalFile));
         } catch (Exception e) {
         }
         Field[] declaredFields = base.getDeclaredFields();
         for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
               try {
                  if (!field.isAccessible()) {
                     field.setAccessible(true);
                  }
                  if (field.getType().equals(Integer.TYPE)) {
                     p.setProperty(field.getName(), field.getInt(null) + "");
                  } else if (field.getType().equals(Double.TYPE)) {
                     p.setProperty(field.getName(), field.getDouble(null) + "");
                  } else if (field.getType().equals(Boolean.TYPE)) {
                     p.setProperty(field.getName(), field.getBoolean(null) + "");
                  } else if (field.getType().equals(Long.TYPE)) {
                     p.setProperty(field.getName(), field.getLong(null) + "");
                  } else if (field.getType().equals(String.class)) {
                     p.setProperty(field.getName(), (String) field.get(null));
                  } else if (field.getType().equals(Byte.TYPE)) {
                     p.setProperty(field.getName(), field.getByte(null) + "");
                  } else if (field.getType().equals(Short.TYPE)) {
                     p.setProperty(field.getName(), field.getShort(null) + "");
                  } else if (field.getType().equals(Float.TYPE)) {
                     p.setProperty(field.getName(), field.getFloat(null) + "");
                  } else if (field.getType().equals(Character.TYPE)) {
                     p.setProperty(field.getName(), field.getChar(null) + "");
                  }
               } catch (Exception e) {
               }
            }
         }
         p.store(new FileOutputStream(CanonicalFile), "");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   /**
    * Reads the static non final fields and return all of type
    * String, Double, Boolean, Long, Byte, Float, Short, Character
    *
    * @param base - The class to get the fields from
    * @return ArrayList<Field> containing all option fields
    */
   public static ArrayList<Field> returnSettingsFields(Class<?> base) {
      ArrayList<Field> fields = new ArrayList<Field>();
      try {
         Field[] declaredFields = base.getDeclaredFields();
         for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
               try {
                  if (!field.isAccessible()) {
                     field.setAccessible(true);
                  }

                  if (
                           field.getType().equals(String.class)  ||
                           field.getType().equals(Integer.TYPE)  || 
                           field.getType().equals(Double.TYPE)   ||
                           field.getType().equals(Boolean.TYPE)  ||
                           field.getType().equals(Long.TYPE)     ||
                           field.getType().equals(Byte.TYPE)     ||
                           field.getType().equals(Float.TYPE)    ||
                           field.getType().equals(Short.TYPE)    ||
                           field.getType().equals(Character.TYPE)
                     )
                  { fields.add(field); }
               } catch (Exception e) {
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return fields;
   }
   
   public static void readGuiConsoleSettings() {
      readSettings(GuiConsole.class, GUI_SETTINGS_FILE);
   }
   
   public static void writeGuiConsoleSettings() {
      writeSettings(GuiConsole.class, GUI_SETTINGS_FILE);
   }
   
   public static void resetGuiConsoleSettings() {
      writeSettings(GuiConsole.class, GUI_SETTINGS_DEFAULT_FILE);
   }
}
