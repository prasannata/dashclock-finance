package com.prasanna.android.dashclock.finance;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.net.Uri;
import android.net.Uri.Builder;
import android.util.Log;

public class HttpHelper {
  private static final String TAG = HttpHelper.class.getSimpleName();

  public interface ResponseParser<T> {
    T parse(String responseBody);
  }

  public <T> T executeHttpGet(String host, String path, Map<String, String> queryParams,
      ResponseParser<T> responseParser) {
    HttpResponse httpResponse = executeRequest(getHttpGetObject(buildUri(host, path, queryParams)));

    if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      try {
        HttpEntity entity = httpResponse.getEntity();
        String jsonText = EntityUtils.toString(entity, HTTP.UTF_8);
        return responseParser.parse(jsonText);
      } catch (ParseException e) {
      } catch (IOException e) {
      }
    }

    return null;
  }

  public String executeHttpGetAndGetResponseBody(String host, String path, Map<String, String> queryParams) {
    HttpResponse httpResponse = executeRequest(getHttpGetObject(buildUri(host, path, queryParams)));

    if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      try {
        return EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
      } catch (ParseException e) {
      } catch (IOException e) {
      }
    }
    return null;
  }

  private HttpResponse executeRequest(HttpRequestBase request) {
    try {
      Log.d(TAG, "Request URI: " + request.getURI());
      HttpClient client = new DefaultHttpClient();
      HttpResponse httpResponse = client.execute(request);
      return httpResponse;
    } catch (ClientProtocolException e) {
    } catch (IOException e) {
    }

    return null;
  }

  protected HttpGet getHttpGetObject(String absoluteUrl) {
    return new HttpGet(absoluteUrl);
  }

  protected String buildUri(String host, String path, Map<String, String> queryParams) {
    Builder uriBuilder = Uri.parse(host).buildUpon();

    if (path != null)
      uriBuilder.appendPath(path);

    if (queryParams != null) {
      for (Map.Entry<String, String> entrySet : queryParams.entrySet())
        uriBuilder.appendQueryParameter(entrySet.getKey(), entrySet.getValue());
    }

    return uriBuilder.build().toString();
  }

}
