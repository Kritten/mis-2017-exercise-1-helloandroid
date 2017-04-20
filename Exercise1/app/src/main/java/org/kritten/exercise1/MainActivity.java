package org.kritten.exercise1;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    final Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Called when the user taps the Send button */
    public void sendMessage(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        String string_url = editText.getText().toString();
//        for convenience reasons, prepend 'http://'
        if (!string_url.startsWith("http://"))
        {
            string_url = "http://" + string_url;
        }
        new GetURL().execute(string_url);
    }

    private class GetURL extends AsyncTask<String, String, ReturnType> {

        protected void onPreExecute () {
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ReturnType doInBackground(String... string_urls) {
            String string_url = string_urls[0];
            ReturnType result = new ReturnType();
            HttpURLConnection urlConnection = null;
            try {
                System.out.println(string_url);
                URL url = new URL(string_url);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = openConnectionCheckRedirects(urlConnection);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int c;
                while ((c = in.read()) != -1) {
                    buffer.write(c);
                }
                buffer.close();
                in.close();

                result.data = buffer.toByteArray();
                String contentType = urlConnection.getContentType();
//                FIX if no header is sent
                if(contentType == null) {
                    contentType = "text/html";
                }
                result.contentType = contentType;

            } catch (SocketTimeoutException e) {
                System.out.println("timeout");
                publishProgress("timeout");
            } catch (MalformedURLException e) {
                System.out.println("malformed url");
                publishProgress("malformed url");
            } catch (ConnectException e) {
                System.out.println("connection refused");
                publishProgress("connection refused");
            } catch (UnknownHostException e) {
                System.out.println("unknown host/no connection");
                publishProgress("unknown host/no connection");
            } catch (FileNotFoundException e) {
                System.out.println("file not found");
                publishProgress("file not found");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("io exception");
                publishProgress("io exception");
            } finally {
                urlConnection.disconnect();
            }
            return result;
        }

//        this method was written by orcale
        private InputStream openConnectionCheckRedirects(HttpURLConnection c) throws IOException
        {
            boolean redir;
            int redirects = 0;
            InputStream in = null;
            do
            {
                if (c instanceof HttpURLConnection)
                {
                    ((HttpURLConnection) c).setInstanceFollowRedirects(false);
                }
                // We want to open the input stream before getting headers
                // because getHeaderField() et al swallow IOExceptions.
                in = c.getInputStream();
                redir = false;
                if (c instanceof HttpURLConnection)
                {
                    HttpURLConnection http = (HttpURLConnection) c;
                    int stat = http.getResponseCode();
                    if (stat >= 300 && stat <= 307 && stat != 306 &&
                            stat != HttpURLConnection.HTTP_NOT_MODIFIED)
                    {
                        URL base = http.getURL();
                        String loc = http.getHeaderField("Location");
                        URL target = null;
                        if (loc != null)
                        {
                            target = new URL(base, loc);
                        }
                        http.disconnect();
                        // Redirection should be allowed only for HTTP and HTTPS
                        // and should be limited to 5 redirections at most.
                        if (target == null || !(target.getProtocol().equals("http")
                                || target.getProtocol().equals("https"))
                                || redirects >= 5)
                        {
                            throw new SecurityException("illegal URL redirect");
                        }
                        redir = true;
                        c = (HttpURLConnection) target.openConnection();
                        redirects++;
                    }
                }
            }
            while (redir);
            return in;
        }

        protected void onProgressUpdate(String... progress) {
//            Activity activity = this;
            Toast.makeText(activity, progress[0], Toast.LENGTH_SHORT).show();


        }
        protected void onPostExecute(ReturnType result) {
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar);
            progressBar.setVisibility(View.GONE);

            if(result.data != null) {
                System.out.println(result.contentType);
                if(result.contentType.contains("text/html") || result.contentType.contains("application/octet-stream")) {
                    ImageView imageView = (ImageView) findViewById(R.id.imageview);
                    WebView webview = (WebView) findViewById(R.id.webview);
                    webview.loadUrl("about:blank");
                    imageView.setVisibility(View.INVISIBLE);
                    webview.setVisibility(View.VISIBLE);
                    try {
                        String html = new String(result.data, "UTF-8");
                        webview.loadData(html, "text/html", "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    WebView webview = (WebView) findViewById(R.id.webview);
                    webview.setVisibility(View.INVISIBLE);
                    webview.loadUrl("about:blank");
                    ImageView imageView = (ImageView) findViewById(R.id.imageview);
                    imageView.setVisibility(View.VISIBLE);
                    Bitmap bMap = BitmapFactory.decodeByteArray(result.data, 0, result.data.length);
                    imageView.setImageBitmap(bMap);
                }
            }
        }
    }

    public class ReturnType {
        public byte[] data;
        public String contentType;
    }
}
