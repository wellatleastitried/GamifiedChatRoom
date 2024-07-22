package com.walit.lifeServer;

import java.net.Socket;
import org.junit.Before;
import org.junit.Test;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;

public class TestServer {

    private ServerDriver driver;
    private ServerDriver.ClientHandler handler;
    private Socket mockSocket;

    @Before
    public void setup() {
        driver = new ServerDriver();
        mockSocket = Mockito.mock(Socket.class);
        handler = driver.new ClientHandler(mockSocket);
    }

    @Test
    public void testFrameGeneration() {
        /*
           Rules of Conway's Game of Life
           1. Any live cell with fewer than two live neighbors dies
           2. Any live cell with two or three live neighbors lives
           3. Any live cell with more than three live neighbors dies
           4. Any dead cell with exactly three live neighbors becomes alive
        */
        // TODO: verify that the server is generating the correct frames to display
        // 0,0,0    0,0,0
        // 0,1,1    0,1,1
        // 0,1,0    0,1,1
        int[][] startFrame = new int[][] {
            {0,0,0},
            {0,1,1},
            {0,1,0}
        };
        int[][] expectedResult = new int[][] {
            {0,0,0},
            {0,1,1},
            {0,1,1}
        };
        MatcherAssert.assertThat(handler.generateNextFrame(startFrame), is(expectedResult));

        // 0,1,1
        // 1,1,1
        // 1,1,1
        //
        // 0,0,1
        // 0,1,0
        // 0,0,1
    }

    @Test
    public void testRandomNumberGen() {
        for (int i = 0; i < 9999; i++) {
            MatcherAssert.assertThat(handler.getRandomDigit(), Matchers.anyOf(is(0), is(1)));
        }
    }

    @Test
    public void testSpeedCheck() {
        for (int i = 1; i <=5; i++) {
            MatcherAssert.assertThat(handler.isValidSpeed(i), is(true));
        }
        for (int i = 0; i < 36; i+=6) {
            MatcherAssert.assertThat(handler.isValidSpeed(i), is(false));
        }
    }

    @Test
    public void testDeserialize() {
        handler.ySize = 4;
        handler.xSize = 5;
        int[][] expectedFrame = new int[][] {
            {0,0,0,0,0},
            {0,0,0,0,0},
            {0,0,0,0,0},
            {0,0,0,0,0}
        };
        String serializedZeros = "0,0,0,0,0;0,0,0,0,0;0,0,0,0,0;0,0,0,0,0;";
        MatcherAssert.assertThat(handler.deserializeStartBoard(serializedZeros), is(expectedFrame));
        for (int i = 0; i < expectedFrame.length; i++) {
            for (int j = 0; j < expectedFrame[i].length; j++) {
                expectedFrame[i][j] = 1;
            }
        }
        String serializedOnes = "1,1,1,1,1;1,1,1,1,1;1,1,1,1,1;1,1,1,1,1;";
        MatcherAssert.assertThat(handler.deserializeStartBoard(serializedOnes), is(expectedFrame));
    }

}
