
package com.walit.lifeClient;

import org.junit.Test;
import org.hamcrest.MatcherAssert;

import static org.hamcrest.CoreMatchers.is;

public class TestClient {

    @Test
    public void canShutdownWithoutRunCalled() {
        MatcherAssert.assertThat(new ClientDriver().shutdown(), is(1));
    }

    @Test
    public void connectionIsAlive() {
        MatcherAssert.assertThat(new ClientDriver().KEEP_ALIVE, is(true));
    }
}
