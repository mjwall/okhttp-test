package com.mjwall;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class OkHttpTest {

    class MyResponse {
        final private int code;
        final private String body;

        public MyResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public int getCode() {
            return this.code;
        }

        public String getBody() {
            return this.body;
        }

        public String toString() {
            return this.code + " -> " + this.body;
        }
    }

    class CrateClient {

        private OkHttpClient client = new OkHttpClient.Builder().build();

        public MyResponse getBlob(String url) throws IOException {
            Request request = new Request.Builder().url(url).addHeader("Connection","close").build();
            Call call = client.newCall(request);
            Response response = call.execute();
            String body = "";
            int code = response.code();
            if (code == 200) {
                body = response.body().string();
            } else {
                // this fixes the issue but seems wrong
                // if you uncomment, fix the 3rd call in test2CallsAfter404
                //client.connectionPool().evictAll();
                response.body().close();
            }
            return new MyResponse(code, body);
        }
    }

    class SomeUse {

        private CrateClient client = new CrateClient();

        public String doIt(String url) {
            try {
                MyResponse response = client.getBlob(url);
                return response.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return "ERROR " + e.getMessage();
            }
        }
    }

    private SomeUse just = null;
    private String url_A = "http://127.0.0.1:4200/_blobs/myblob/6dcd4ce23d88e2ee9568ba546c007c63d9131c1b";
    private String url_404 = "http://127.0.0.1:4200/_blobs/myblob/ae4f281df5a5d0ff3cad6371f76d5c29b6d953ec"; //sha1 of B
    private String url_C = "http://127.0.0.1:4200/_blobs/myblob/32096c2e0eff33d844ee6d675407ace18289357d";
    private String url_D = "http://127.0.0.1:4200/_blobs/myblob/50c9e8d5fc98727b4bbc93cf5d64a68db647f04f";

    @Before
    public void setUp() {
        just = new SomeUse();
    }

    @After
    public void tearDown() {
        just = null;
    }

    @Test
    public void test200() {
        assertEquals("200 -> A", just.doIt(url_A));
    }

    @Test
    public void test404() {
        assertEquals("404 -> ", just.doIt(url_404));
    }

    @Test
    public void test200Then404() {
        assertEquals("200 -> A", just.doIt(url_A));
        assertEquals("404 -> ", just.doIt(url_404));
    }

    @Test
    public void testCallAfter404() {
        assertEquals("200 -> A", just.doIt(url_A));
        assertEquals("404 -> ", just.doIt(url_404));
        // this is a 404 but should be a 200 with A, which we called 2 lines earlier
        assertEquals("200 -> A", just.doIt(url_A));
    }

    @Test
    public void test2CallsAfter404() {
        assertEquals("200 -> A", just.doIt(url_A));
        assertEquals("404 -> ", just.doIt(url_404));
        // this should be a 200 but as shown in the prior test it is a 404
        //assertEquals("404 -> ", just.doIt(url_C));
        assertEquals("200 -> C", just.doIt(url_C));
        // this will be a 200 but the content is C not D
        assertEquals("200 -> D", just.doIt(url_D));
    }

}
