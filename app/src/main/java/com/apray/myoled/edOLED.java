package com.apray.myoled;
//Ported c code form https://github.com/sparkfun/Edison_OLED_Block/blob/master/Firmware/pong/gpio/gpio_edison.cpp
//changed how the screen was displayed, and added some new features.

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.lang.Thread.sleep;

/**
 * Created by apray on 6/3/2017.
 */
/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */

public class edOLED {

    // SPI Device Name
    private static final String SPI_DEVICE_NAME = "SPI2.1";
    private static final String TAG = "edOLED";
    private Gpio DC_PIN;
    private Gpio RST_PIN;

    private ButtonInputDriver Up_PIN;
    private ButtonInputDriver Down_PIN;
    private ButtonInputDriver Left_PIN;
    private ButtonInputDriver Right_PIN;

    private ButtonInputDriver Select_PIN;

    private ButtonInputDriver A_PIN;
    private ButtonInputDriver B_PIN;

    private static final char[][] fontsPointer = {fonts.font5x7, fonts.font8x16, fonts.sevensegment, fonts.fontlargenumber};


    char drawMode,fontWidth, fontHeight, fontType, fontStartChar, cursorX, cursorY;
    int fontMapWidth,fontTotalChar;
    boolean foreColor;

    private static final boolean BLACK = false;
    private static final boolean WHITE = true;

    private static final char LCDWIDTH			=64;
    private static final char LCDHEIGHT			=48;
    private static final char FONTHEADERSIZE		=6;

    public static final char NORM				=0;
    public static final char XOR					=1;

    public static final char PAGE				=0;
    public static final char ALL					=1;

    private static final char SETCONTRAST 		=0x81;
    private static final char DISPLAYALLONRESUME 	=0xA4;
    private static final char DISPLAYALLON 		=0xAf5;
    private static final char NORMALDISPLAY 		=0xA6;
    private static final char INVERTDISPLAY 		=0xA7;
    private static final char DISPLAYOFF 			=0xAE;
    private static final char DISPLAYON 			=0xAF;
    private static final char SETDISPLAYOFFSET 	=0xD3;
    private static final char SETCOMPINS 			=0xDA;
    private static final char SETVCOMDESELECT		=0xDB;
    private static final char SETDISPLAYCLOCKDIV 	=0xD5;
    private static final char SETPRECHARGE 		=0xD9;
    private static final char SETMULTIPLEX 		=0xA8;
    private static final char SETLOWCOLUMN 		=0x00;
    private static final char SETHIGHCOLUMN 		=0x10;
    private static final char SETSTARTLINE 		=0x40;
    private static final char MEMORYMODE 			=0x20;
    private static final char COMSCANINC 			=0xC0;
    private static final char COMSCANDEC 			=0xC8;
    private static final char SEGREMAP 			=0xA0;
    private static final char CHARGEPUMP 			=0x8D;
    private static final char EXTERNALVCC 		=0x01;
    private static final char SWITCHCAPVCC 		=0x02;

// Scroll
    private static final char ACTIVATESCROLL 					=0x2F;
    private static final char DEACTIVATESCROLL 				=0x2E;
    private static final char SETVERTICALSCROLLAREA 			=0xA3;
    private static final char RIGHTHORIZONTALSCROLL 			=0x26;
    private static final char LEFT_HORIZONTALSCROLL 			=0x27;
    private static final char VERTICALRIGHTHORIZONTALSCROLL	=0x29;
    private static final char VERTICALLEFTHORIZONTALSCROLL	=0x2A;

    public  class cmd {
        private static final char CMD_CLEAR = 0;
        private static final char CMD_INVERT = 1;
        private static final char CMD_CONTRAST = 2;
        private static final char CMD_DISPLAY = 3;
        private static final char CMD_SETCURSOR = 4;
        private static final char CMD_PIXEL = 5;
        private static final char CMD_LINE = 6;
        private static final char CMD_LINEH = 7;
        private static final char CMD_LINEV = 8;
        private static final char CMD_RECT = 9;
        private static final char CMD_RECTFILL = 10;
        private static final char CMD_CIRCLE = 11;
        private static final char CMD_CIRCLEFILL = 12;
        private static final char CMD_DRAWCHAR = 13;
        private static final char CMD_DRAWBITMAP = 14;
        private static final char CMD_GETLCDWIDTH = 15;
        private static final char CMD_GETLCDHEIGHT = 16;
        private static final char CMD_SETCOLOR = 17;
        private static final char CMD_SETDRAWMODE = 18;
    }
    private static final boolean    LOW =false;
    private static final boolean    HIGH =true;
    private static final char    NONE =2;

    private SpiDevice oledSPI;

    // Change the total fonts included
    int TOTALFONTS = 4;
    int recvLEN=100;
    byte[] serInStr = new byte[recvLEN];
    char[] serCmd = new char[recvLEN];
    /*
    char fontsPointer[]={
                font5x7
                ,font8x16
                ,sevensegment
                ,fontlargenumber
    };
    */

    /** \brief edOLED screen buffer.
     Page buffer 64 x 48 divided by 8 = 384 chars
     Page buffer is required because in SPI mode, the host cannot read the SSD1306's
     GDRAM of the controller.  This page buffer serves as a scratch RAM for graphical
     functions.  All drawing function will first be drawn on this page buffer, only
     upon calling display() function will transfer the page buffer to the actual LCD
     controller's memory.
     */
    char screenmemory [] = {
	/* LCD Memory organised in 64 horizontal pixel and 6 rows of char
	 B  B .............B  -----
	 y  y .............y        \
	 t  t .............t         \
	 e  e .............e          \
	 0  1 .............63          \
	                                \
	 D0 D0.............D0            \
	 D1 D1.............D1            / ROW 0
	 D2 D2.............D2           /
	 D3 D3.............D3          /
	 D4 D4.............D4         /
	 D5 D5.............D5        /
	 D6 D6.............D6       /
	 D7 D7.............D7  ----
	*/
            //SparkFun Electronics LOGO

            // ROW0, char0 to char63
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xE0, 0xF8, 0xFC, 0xFE, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0x0F, 0x07, 0x07, 0x06, 0x06, 0x00, 0x80, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            // ROW1, char64 to char127
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x81, 0x07, 0x0F, 0x3F, 0x3F, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFE, 0xFE, 0xFC, 0xFC, 0xFC, 0xFE, 0xFF, 0xFF, 0xFF, 0xFC, 0xF8, 0xE0,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            // ROW2, char128 to char191
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFC,
            0xFE, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xF1, 0xE0, 0xE0, 0xE0, 0xE0, 0xE0, 0xF0, 0xFD, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            // ROW3, char192 to char255
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x7F, 0x3F, 0x1F, 0x07, 0x01,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            // ROW4, char256 to char319
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x7F, 0x3F, 0x1F, 0x1F, 0x0F, 0x0F, 0x0F, 0x0F,
            0x0F, 0x0F, 0x0F, 0x0F, 0x07, 0x07, 0x07, 0x03, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            // ROW5, char320 to char383
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF,
            0x7F, 0x3F, 0x1F, 0x0F, 0x07, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    private ImageReader imgRnd;
    public LinearLayout mylayout;
    edOLED()
    {
        this((LinearLayout)null);
    }

    edOLED(LinearLayout _layout)
    {
        mylayout = _layout;
        try {
            PeripheralManagerService manager = new PeripheralManagerService();
            oledSPI = manager.openSpiDevice(SPI_DEVICE_NAME);
            oledSPI.setMode(SpiDevice.MODE0);


            oledSPI.setFrequency(10000000);     // 16MHz
            //GP109, GP110, GP111, GP114, GP115, GP12, GP128, GP129, GP13, GP130, GP131, GP134, GP135, GP14, GP15, GP165, GP182, GP183, GP19, GP20, GP27, GP28, GP40, GP41, GP42, GP43, GP44, GP45, GP46, GP47, GP48, GP49, GP78, GP79, GP80, GP81, GP82, GP83, GP84
            RST_PIN = manager.openGpio("GP15");
            DC_PIN = manager.openGpio("GP14");
            DC_PIN.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            RST_PIN.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            Up_PIN = new ButtonInputDriver("GP47",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_DPAD_UP);
            Up_PIN.register();

            Down_PIN = new ButtonInputDriver("GP44",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_DPAD_DOWN);
            Down_PIN.register();

            Left_PIN = new ButtonInputDriver("GP165",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_DPAD_LEFT);
            Left_PIN.register();

            Right_PIN = new ButtonInputDriver("GP45",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_DPAD_RIGHT);
            Right_PIN.register();



            Select_PIN = new ButtonInputDriver("GP48",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            Select_PIN.register();



            A_PIN = new ButtonInputDriver("GP49",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_A);
            A_PIN.register();

            B_PIN = new ButtonInputDriver("GP46",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_B);
            B_PIN.register();

            //mDevice.setBitsPerWord(8);          // 8 BPW
            //mDevice.setBitJustification(false); // MSB first
        } catch (IOException e) {
            Log.w(TAG, "Unable to access SPI device", e);
        }
    }

    void onDestroy() {

        if (oledSPI != null) {
            try {
                oledSPI.close();
                oledSPI = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close SPI device", e);
            }
        }
    }


    /** \brief Initialization of edOLED Library.
     Setup IO pins for SPI port then send initialization commands to the
     SSD1306 controller inside the OLED.
     */
    void begin() throws InterruptedException {
        // default 5x7 font
        setFontType((char)(0));
        setColor(WHITE);
        setDrawMode(NORM);
        setCursor((char)(0),(char)(0));

        spiSetup();
        try {
            RST_PIN.setValue(HIGH); //(digitalWrite(rstPin, HIGH);
            sleep(5); // VDD (3.3V) goes high at start, lets just chill for 5 ms
            RST_PIN.setValue(LOW); // bring reset low
            sleep(10); // wait 10ms
            RST_PIN.setValue(HIGH);	//digitalWrite(rstPin, HIGH);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Init sequence for 64x48 OLED module
        command(DISPLAYOFF);			// 0xAE

        command(SETDISPLAYCLOCKDIV);	// 0xD5
        command((char)(0x80));					// the suggested ratio 0x80

        command(SETMULTIPLEX);			// 0xA8
        command((char)(0x2F));

        command(SETDISPLAYOFFSET);		// 0xD3
        command((char)(0x0));					// no offset

        command((char)(SETSTARTLINE | 0x0));	// line #0

        command(CHARGEPUMP);			// enable charge pump
        command((char)(0x14));

        command(NORMALDISPLAY);			// 0xA6
        command(DISPLAYALLONRESUME);	// 0xA4

        command((char)(SEGREMAP | 0x1));
        command(COMSCANDEC);

        command(SETCOMPINS);			// 0xDA
        command((char)(0x12));

        command(SETCONTRAST);			// 0x81
        command((char)(0x8F));

        command(SETPRECHARGE);			// 0xd9
        command((char)(0xF1));

        command(SETVCOMDESELECT);			// 0xDB
        command((char)(0x40));

        command(DISPLAYON);				//--turn on oled panel
        clear(ALL);						// Erase hardware memory inside the OLED
        imgRnd = ImageReader.newInstance(getLCDWidth(),getLCDHeight(), PixelFormat.RGBA_8888,5);
        imgRnd.setOnImageAvailableListener(ImageAvailableListener,null);
        buildLayout();
    }


    public void drawLayout(){
        if(mylayout == null) return;
        Canvas canvas = imgRnd.getSurface().lockCanvas(new Rect(imgRnd.getWidth(), imgRnd.getHeight(), 0, 0));
        mylayout.draw(canvas);
        imgRnd.getSurface().unlockCanvasAndPost(canvas);
    }

    private void buildLayout(){
        if(mylayout == null) return;

        Canvas canvas = imgRnd.getSurface().lockCanvas(new Rect(imgRnd.getWidth(), imgRnd.getHeight(), 0, 0));
        mylayout.measure(canvas.getWidth(), canvas.getHeight());

        mylayout.layout(0, 0, canvas.getWidth(), canvas.getHeight());
        mylayout.draw(canvas);
        imgRnd.getSurface().unlockCanvasAndPost(canvas);
    }

    ImageReader.OnImageAvailableListener ImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        int count =0;
        final float factor = 255f;
        final float redBri = 0.2126f;
        final float greenBri = 0.2126f;
        final float blueBri = 0.0722f;

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "in OnImageAvailable");
            Image img = null;

            try {
                img = reader.acquireLatestImage();
                if (img != null) {
                    Image.Plane[] planes = img.getPlanes();
                    if (planes[0].getBuffer() == null) {
                        return;
                    }
                    int width = img.getWidth();
                    int height = img.getHeight();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    byte[] newData = new byte[width * height * 4];

                    int offset = 0;
                    ByteBuffer buffer = planes[0].getBuffer();
                    for (int i = 0; i < height; ++i) {
                        for (int j = 0; j < width; ++j) {
                            int pixel = 0;
                            int r = (buffer.get(offset) & 0xff) ;
                            int g = (buffer.get(offset + 1) & 0xff) ;
                            int b = (buffer.get(offset + 2) & 0xff);
                            int a = (buffer.get(offset + 3) & 0xff) ;

                            pixel |= r << 16;     // R
                            pixel |= g << 8;  // G
                            pixel |= b;       // B
                            pixel |= a << 24; // A

                            wrpixel((char) j,(char) i, pixel > 0 ,NORM);
                            offset += pixelStride;
                        }
                        offset += rowPadding;
                    }
                    display();
                    img.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != img) {
                    img.close();
                }

            }
        }
    };

    /** \brief SPI command.
     Setup DC and SS pins, then send command via SPI to SSD1306 controller.
     */
    void command(char c)
    {
        try {
            DC_PIN.setValue(LOW); // DC pin LOW
            spiTransfer(c);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /** \brief SPI data.
     Setup DC and SS pins, then send data via SPI to SSD1306 controller.
     */
    void data(byte[] c,int len)
    {
        try {
            DC_PIN.setValue(HIGH);	// DC HIGH
            spiTransfer(c,len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void data(char c)
    {
        try {
            DC_PIN.setValue(HIGH);	// DC HIGH
            spiTransfer(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** \brief Set SSD1306 page address.
     Send page address command and address to the SSD1306 OLED controller.
     */
    void setPageAddress(char add)
    {
        add=(char)(0xb0|add);
        command(add);
        return;
    }

    /** \brief Set SSD1306 column address.
     Send column address command and address to the SSD1306 OLED controller.
     */
    void setColumnAddress(char add)
    {
        command((char)((0x10|(add>>4))+0x02));
        command((char)((0x0f&add)));
        return;
    }

    /** \brief Clear screen buffer or SSD1306's memory.

     To clear GDRAM inside the LCD controller, pass in the variable mode =
     ALL and to clear screen page buffer pass in the variable mode = PAGE.
     */
    void clear(char mode)
    {
        //	char page=6, col=0x40;
        if (mode==ALL)
        {
            for (int i=0;i<8; i++)
            {
                setPageAddress((char)(i));
                setColumnAddress((char)(0));
                byte[] screnPage= new byte[0x80];
                data(screnPage,0x80);
            }
        }
        else
        {
            for(int i=0; i<384; i++){
                screenmemory[i]=0;
            }
        }
    }

    /** \brief Clear or replace screen buffer or SSD1306's memory with a character.	
     To clear GDRAM inside the LCD controller, pass in the variable mode = ALL
     with c character and to clear screen page buffer, pass in the variable
     mode = PAGE with c character.
     */
    void clear(char mode, char c)
    {
        byte[] screenPage = new byte[0x80];

        //char page=6, col=0x40;
        if (mode==ALL)
        {
            for (int i=0;i<8; i++)
            {
                setPageAddress((char)(i));
                setColumnAddress((char)(0));

                for (int j=0; j<0x80; j++)
                {
                    screenPage[j]=(byte)c;
                }
                data(screenPage,0x80);
            }
        }
        else
        {
            for(int i=0; i<384; i++){
                screenmemory[i]=c;
            }
            display();
        }
    }

    /** \brief Invert display.
     The WHITE color of the display will turn to BLACK and the BLACK will turn
     to WHITE.
     */
    void invert(char inv)
    {
        if (inv == 1)
            command(INVERTDISPLAY);
        else
            command(NORMALDISPLAY);
    }

    /** \brief Set contrast.
     OLED contract value from 0 to 255. Note: Contrast level is not very obvious.
     */
    void contrast(char contrast)
    {
        command(SETCONTRAST);			// 0x81
        command(contrast);
    }

    /** \brief Transfer display memory.
     Bulk move the screen buffer to the SSD1306 controller's memory so that images/graphics drawn on the screen buffer will be displayed on the OLED.
     */
    void display()
    {
        char i, j;

        byte[] screenPage = new byte[0x40];
        int t=0;
        for (i=0; i<6; i++)
        {
            setPageAddress(i);
            setColumnAddress((char)(0));
            for (j=0;j<0x40;j++)
            {
                screenPage[t++]=(byte)screenmemory[i*0x40+j];

            }
            data(screenPage,t);
            t=0;
        }


    }

/** \brief write a character to the display
 */
    char write(char c)
    {
        if (c == '\n')
        {
            cursorY += fontHeight;
            cursorX  = 0;
        }
        else if (c == '\r')
        {
            // skip
        }
        else
        {
            drawChar(cursorX, cursorY, c, foreColor, drawMode);
            cursorX += fontWidth+1;
            if ((cursorX > (LCDWIDTH - fontWidth)))
            {
                cursorY += fontHeight;
                cursorX = 0;
            }
        }

        return 1;
    }

    void print (String str){
        print(str.toCharArray());
    }
    void print(char[]  c)
    {
        int len = c.length;

        for (int i=0; i<len; i++)
        {
            write(c[i]);
        }
    }

    void print(int d)
    {
        char[] temp = String.format( "%d", d).toCharArray();
        print(temp);
    }

    void print(float f)
    {
        char[] temp = String.format( "%.2f", f).toCharArray();
        print(temp);
    }

    /** \brief Set cursor position.
     edOLED's cursor position to x,y.
     */
    void setCursor(char x, char y)
    {
        cursorX=x;
        cursorY=y;
    }


    /** \brief Draw line.
     Draw line using current fore color and current draw mode from x0,y0 to x1,y1 of the screen buffer.
     */

    void line(char x0, char y0, char x1, char y1)
    {
        line(x0,y0,x1,y1,foreColor,drawMode);
    }

    /** \brief Draw line with color and mode.
     Draw line using color and mode from x0,y0 to x1,y1 of the screen buffer.
     */
    void line(char x0, char y0, char x1, char y1, boolean color, char mode)
    {
        boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
        char temp;
        if (steep)
        {

            temp = x0;
            x0 = y0;
            y0 = temp;

            temp = x1;
            x1 = y1;
            y1 = temp;
        }

        if (x0 > x1)
        {
            temp = x0;
            x0 = x1;
            x1 = temp;

            temp = y0;
            y0 = y1;
            y1 = temp;
        }

        char dx, dy;
        dx = (char)(x1 - x0);
        dy = (char)(Math.abs(y1 - y0));

        int err = dx / 2;
        int ystep;

        if (y0 < y1)
        {
            ystep = 1;
        }
        else
        {
            ystep = -1;
        }

        for (; x0<x1; x0++)
        {
            if (steep)
            {
                wrpixel(y0, x0, color, mode);
            } else
            {
                wrpixel(x0, y0, color, mode);
            }
            err -= dy;
            if (err < 0)
            {
                y0 += ystep;
                err += dx;
            }
        }
    }

    /** \brief Draw horizontal line.
     Draw horizontal line using current fore color and current draw mode from x,y to x+width,y of the screen buffer.
     */
    void lineH(char x, char y, char width)
    {
        line(x,y,(char)(x+width),y,foreColor,drawMode);
    }

    /** \brief Draw horizontal line with color and mode.
     Draw horizontal line using color and mode from x,y to x+width,y of the screen buffer.
     */
    void lineH(char x, char y, char width, boolean color, char mode)
    {
        line(x,y,(char)(x+width),y,color,mode);
    }

    /** \brief Draw vertical line.
     Draw vertical line using current fore color and current draw mode from x,y to x,y+height of the screen buffer.
     */
    void lineV(char x, char y, char height)
    {
        line(x,y,x,(char)(y+height),foreColor,drawMode);
    }

    /** \brief Draw vertical line with color and mode.
     Draw vertical line using color and mode from x,y to x,y+height of the screen buffer.
     */
    void lineV(char x, char y, char height, boolean color, char mode)
    {
        line(x,y,x,(char)(y+height),color,mode);
    }

    /** \brief Draw rectangle.
     Draw rectangle using current fore color and current draw mode from x,y to x+width,y+height of the screen buffer.
     */
    void rect(char x, char y, char width, char height)
    {
        rect(x,y,width,height,foreColor,drawMode);
    }

    /** \brief Draw rectangle with color and mode.
     Draw rectangle using color and mode from x,y to x+width,y+height of the screen buffer.
     */
    void rect(char x, char y, char width, char height, boolean color , char mode)
    {
        char tempHeight;

        lineH(x,y, width, color, mode);
        lineH(x,(char)(y+height-1), width, color, mode);

        tempHeight=(char)(height-2);

        // skip drawing vertical lines to avoid overlapping of pixel that will
        // affect XOR plot if no pixel in between horizontal lines
        if (tempHeight<1)
            return;

        lineV(x,(char)(y+1), tempHeight, color, mode);
        lineV((char)(x+width-1), (char)(y+1), tempHeight, color, mode);
    }

    /** \brief Draw filled rectangle.
     Draw filled rectangle using current fore color and current draw mode from x,y to x+width,y+height of the screen buffer.
     */
    void rectFill(char x, char y, char width, char height)
    {
        rectFill(x,y,width,height,foreColor,drawMode);
    }

    /** \brief Draw filled rectangle with color and mode.
     Draw filled rectangle using color and mode from x,y to x+width,y+height of the screen buffer.
     */
    void rectFill(char x, char y, char width, char height, boolean color , char mode)
    {
        for (int i=x; i<x+width;i++)
        {
            lineV((char)(i),y, height, color, mode);
        }
    }

    /** \brief Draw circle.
     Draw circle with radius using current fore color and current draw mode at x,y of the screen buffer.
     */
    void circle(char x0, char y0, char radius)
    {
        circle(x0,y0,radius,foreColor,drawMode);
    }

    /** \brief Draw circle with color and mode.
     Draw circle with radius using color and mode at x,y of the screen buffer.
     */
    void circle(char x0, char y0, char radius, boolean color, char mode)
    {
        int f = 1 - radius;
        int ddF_x = 1;
        int ddF_y = -2 * radius;
        int x = 0;
        int y = radius;

        wrpixel(x0, (char)(y0+radius), color, mode);
        wrpixel(x0, (char)(y0-radius), color, mode);
        wrpixel((char)(x0+radius), y0, color, mode);
        wrpixel((char)(x0-radius), y0, color, mode);

        while (x<y)
        {
            if (f >= 0)
            {
                y--;
                ddF_y += 2;
                f += ddF_y;
            }
            x++;
            ddF_x += 2;
            f += ddF_x;

            wrpixel((char)(x0 + x), (char)(y0 + y), color, mode);
            wrpixel((char)(x0 - x), (char)(y0 + y), color, mode);
            wrpixel((char)(x0 + x), (char)(y0 - y), color, mode);
            wrpixel((char)(x0 - x), (char)(y0 - y), color, mode);

            wrpixel((char)(x0 + y), (char)(y0 + x), color, mode);
            wrpixel((char)(x0 - y), (char)(y0 + x), color, mode);
            wrpixel((char)(x0 + y), (char)(y0 - x), color, mode);
            wrpixel((char)(x0 - y), (char)(y0 - x), color, mode);

        }
    }

    /** \brief Draw filled circle.
     Draw filled circle with radius using current fore color and current draw mode at x,y of the screen buffer.
     */
    void circleFill(char x0, char y0, char radius)
    {
        circleFill(x0,y0,radius,foreColor,drawMode);
    }

    /** \brief Draw filled circle with color and mode.
     Draw filled circle with radius using color and mode at x,y of the screen buffer.
     */
    void circleFill(char x0, char y0, char radius, boolean color, char mode)
    {
        int f = 1 - radius;
        int ddF_x = 1;
        int ddF_y = -2 * radius;
        int x = 0;
        int y = radius;

        // Temporary disable fill circle for XOR mode.
        if (mode==XOR) return;

        for (char i=(char)(y0-radius); i<=y0+radius; i++)
        {
            wrpixel(x0, i, color, mode);
        }

        while (x<y)
        {
            if (f >= 0)
            {
                y--;
                ddF_y += 2;
                f += ddF_y;
            }
            x++;
            ddF_x += 2;
            f += ddF_x;

            for (char i=(char)(y0-y); i<=y0+y; i++)
            {
                wrpixel((char)(x0+x), i, color, mode);
                wrpixel((char)(x0-x), i, color, mode);
            }
            for (char i=(char)(y0-x); i<=y0+x; i++)
            {
                wrpixel((char)(x0+y), i, color, mode);
                wrpixel((char)(x0-y), i, color, mode);
            }
        }
    }

/** \brief Get LCD height.
 The height of the LCD return as char.
 */
    char getLCDHeight()
    {
        return LCDHEIGHT;
    }

/** \brief Get LCD width.
 The width of the LCD return as char.
 */
    char getLCDWidth()
    {
        return LCDWIDTH;
    }

/** \brief Get font width.
 The cucrrent font's width return as char.
 */
    char getFontWidth()
    {
        return fontWidth;
    }

/** \brief Get font height.
 The current font's height return as char.
 */
    char getFontHeight()
    {
        return fontHeight;
    }

/** \brief Get font starting character.
 Return the starting ASCII character of the currnet font, not all fonts start with ASCII character 0. Custom fonts can start from any ASCII character.
 */
    char getFontStartChar()
    {
        return fontStartChar;
    }

/** \brief Get font total characters.
 Return the total characters of the current font.
 */
    int getFontTotalChar()
    {
        return fontTotalChar;
    }

/** \brief Get total fonts.
 Return the total number of fonts loaded into the edOLED's flash memory.
 */
    char getTotalFonts()
    {
        return (char)(TOTALFONTS);
    }

/** \brief Get font type.
 Return the font type number of the current font.
 */
    char getFontType()
    {
        return fontType;
    }

/** \brief Set font type.
 Set the current font type number, ie changing to different fonts base on the type provided.
 */
    boolean setFontType(char type)
    {
        if ((type>=TOTALFONTS) || (type<0))
            return false;

        fontType=type;
        fontWidth=fontsPointer[fontType][0];
        fontHeight=fontsPointer[fontType][1];
        fontStartChar=fontsPointer[fontType][2];
        fontTotalChar=fontsPointer[fontType][3];
        fontMapWidth=(fontsPointer[fontType][4]*100)+(fontsPointer[fontType][5]); // two chars values into integer 16

        //Log.d(TAG,"fontType:"+(int)fontType);
        //Log.d(TAG,"fontWidth:"+(int)fontWidth);
        //Log.d(TAG,"fontHeight:"+(int)fontHeight);
        //Log.d(TAG,"fontStartChar:"+(int)fontStartChar);
        //Log.d(TAG,"fontTotalChar:"+(int)fontTotalChar);
        //Log.d(TAG,"fontMapWidth:"+fontMapWidth);

        return true;
    }

    /** \brief Set color.
     Set the current draw's color. Only WHITE and BLACK available.
     */
    void setColor(boolean color)
    {
        foreColor=color;
    }

    /** \brief Set draw mode.
     Set current draw mode with NORM or XOR.
     */
    void setDrawMode(char mode)
    {
        drawMode=mode;
    }

    /** \brief Draw character.
     Draw character c using current color and current draw mode at x,y.
     */
    void  drawChar(char x, char y, char c)
    {
        drawChar(x,y,c,foreColor,drawMode);
    }

    /** \brief Draw character with color and mode.
     Draw character c using color and draw mode at x,y.
     */
    void  drawChar(char x, char y, char c, boolean color, char mode)
    {
        Log.d(TAG,"Drawing Char:"+c);

        int rowsToDraw,row, tempC;
        int i,j,temp;
        int charPerBitmapRow,charColPositionOnBitmap,charRowPositionOnBitmap,charBitmapStartPosition;

        if ((c<fontStartChar) || (c>(fontStartChar+fontTotalChar-1)))		// no bitmap for the required c
            return;

        tempC= c-fontStartChar;
        Log.d(TAG,"tempC:"+tempC);

        // each row (in datasheet is call page) is 8 bits high, 16 bit high character will have 2 rows to be drawn
        rowsToDraw=fontHeight/8;	// 8 is LCD's page size, see SSD1306 datasheet


        if (rowsToDraw<=1) rowsToDraw=1;

        Log.d(TAG,"rowsToDraw:"+rowsToDraw);

        // the following draw function can draw anywhere on the screen, but SLOW pixel by pixel draw
        if (rowsToDraw==1)
        {
            for  (i=0;i<fontWidth+1;i++)
            {
                if (i==fontWidth) // this is done in a weird way because for 5x7 font, there is no margin, this code add a margin after col 5
                    temp=0;
                else
                    temp=fontsPointer[fontType][FONTHEADERSIZE+(tempC*fontWidth)+i];

                //Log.d(TAG,"rowsToDraw:"+rowsToDraw);

                for (j=0;j<8;j++)
                {			// 8 is the LCD's page height (see datasheet for explanation)
                    if ((char)(temp & 0x1) == 1)
                    {
                        wrpixel((char)(x+i), (char)(y+j), color,mode);
                    }
                    else
                    {
                        wrpixel((char)(x+i), (char)(y+j), !color ,mode);
                    }

                    temp >>=1;
                }
            }
            return;
        }

        // font height over 8 bit
        // take character "0" ASCII 48 as example
        charPerBitmapRow=(char)(fontMapWidth/fontWidth);  // 256/8 =32 char per row
        charColPositionOnBitmap=(char)(tempC % charPerBitmapRow);  // =16
        charRowPositionOnBitmap=(char)(tempC/charPerBitmapRow); // =1
        charBitmapStartPosition=(char)((charRowPositionOnBitmap * fontMapWidth * (fontHeight/8)) + (charColPositionOnBitmap * fontWidth)) ;

        // each row on LCD is 8 bit height (see datasheet for explanation)
        for(row=0;row<rowsToDraw;row++)
        {
            for (i=0; i<fontWidth;i++)
            {
                temp=fontsPointer[fontType][FONTHEADERSIZE+(charBitmapStartPosition+i+(row*fontMapWidth))];
                for (j=0;j<8;j++)
                {			// 8 is the LCD's page height (see datasheet for explanation)
                    if ((char)(temp & 0x1)==1)
                    {
                        wrpixel((char)(x+i),(char)(y+j+(row*8)), color, mode);
                    }
                    else
                    {
                        wrpixel((char)(x+i),(char)(y+j+(row*8)), !color, mode);
                    }
                    temp >>=1;
                }
            }
        }

    }

    /** \brief Stop scrolling.
     Stop the scrolling of graphics on the OLED.
     */
    void scrollStop()
    {
        command(DEACTIVATESCROLL);
    }

    /** \brief Right scrolling.
     Set row start to row stop on the OLED to scroll right. Refer to http://learn.edOLED.io/intro/general-overview-of-edOLED.html for explanation of the rows.
     */
    void scrollRight(char start, char stop)
    {
        if (stop<start)		// stop must be larger or equal to start
            return;
        scrollStop();		// need to disable scrolling before starting to avoid memory corrupt
        command(RIGHTHORIZONTALSCROLL);
        command((char)(0x00));
        command(start);
        command((char)(0x7));		// scroll speed frames
        command(stop);
        command((char)(0x00));
        command((char)(0xFF));
        command(ACTIVATESCROLL);
    }

    /** \brief Vertical flip.
     Flip the graphics on the OLED vertically.
     */
    void flipVertical(char flip)
    {
        if (flip == 1)
        {
            command(COMSCANINC);
        }
        else
        {
            command(COMSCANDEC);
        }
    }

    /** \brief Horizontal flip.
     Flip the graphics on the OLED horizontally.
     */
    void flipHorizontal(char flip)
    {
        if (flip ==1)
        {
            command((char)(SEGREMAP | 0x0));
        }
        else
        {
            command((char)(SEGREMAP | 0x1));
        }
    }

    void spiSetup()
    {
	/*
	// Bit-bang method:
	MOSI_PIN.pinMode(OUTPUT);	//pinMode(MOSI, OUTPUT_FAST);
	SCLK_PIN.pinMode(OUTPUT);	//pinMode(SCK, OUTPUT_FAST);
	CS_PIN.pinMode(OUTPUT);		//pinMode(csPin, OUTPUT_FAST);
	CS_PIN.setValue(HIGH);		//digitalWrite(csPin, HIGH);
	*/
    }

    void spiTransfer(byte[] data,int len)
    {
        // SPI library method:
        //CS_PIN.setValue(LOW);	//digitalWrite(csPin, LOW);
        try {
            oledSPI.write(data,len);	//, NULL, 1, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //CS_PIN.setValue(HIGH);	//digitalWrite(csPin, HIGH);
    }
    void spiTransfer(char data)
    {
        byte[] temp = {(byte)(data)};
        // SPI library method:
        //CS_PIN.setValue(LOW);	//digitalWrite(csPin, LOW);
        try {
            oledSPI.write(temp,1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //CS_PIN.setValue(HIGH);	//digitalWrite(csPin, HIGH);
    }

    public void setCursor(int x, int y) {
        this.setCursor((char)x,(char)y);
    }

    public void circle(int x0, int y0, int radius) {
        this.circle((char)x0,(char)y0,(char)radius);
    }

    public boolean rdPixal(int x, int y) {
        int byt = (y % 8);

        int index = x + (y / 8) * LCDWIDTH;
        if (index > screenmemory.length-1) return false;
        if (index < 0) return false;
        char bytes = screenmemory[index];
        ;

        //Log.d(TAG,"index:["+index+":"+byt+"] pix:"+((bytes >> byt) & 1));
        return  ((bytes >> byt) & 1) == 1;
    }

    /** \brief Draw pixel.
     Draw pixel using the current fore color and current draw mode in the screen buffer's x,y position.
     */
    void wrpixel(char x, char y)
    {
        wrpixel(x,y,foreColor,drawMode);
    }

    /** \brief Draw pixel with color and mode.
     Draw color pixel in the screen buffer's x,y position with NORM or XOR draw mode.
     */
    void wrpixel(char x, char y, boolean color, char mode)
    {
        if ((x<0) ||  (x>=LCDWIDTH) || (y<0) || (y>=LCDHEIGHT))
            return;

        if (mode==XOR)
        {
            if (color==WHITE)
                screenmemory[x+ (y/8)*LCDWIDTH] ^= (1<<(y%8));
        }
        else
        {
            if (color==WHITE)
                screenmemory[x+ (y/8)*LCDWIDTH] |= (1<<(y%8));
            else
                screenmemory[x+ (y/8)*LCDWIDTH] &= ~(1<<(y%8));
        }
    }
}
