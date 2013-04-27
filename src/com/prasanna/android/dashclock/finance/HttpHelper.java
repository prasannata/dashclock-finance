package com.prasanna.android.dashclock.finance;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.net.Uri.Builder;
import android.util.Log;

public class HttpHelper
{
    private static final String TAG = HttpHelper.class.getSimpleName();
    
    public JSONObject executeHttpGet(String host, String path, Map<String, String> queryParams)
    {
        HttpGet request = getHttpGetObject(buildUri(host, path, queryParams));
        return executeRequest(new DefaultHttpClient(), request);
    }

    private JSONObject executeRequest(HttpClient client, HttpRequestBase request)
    {
        try
        {
            Log.d(TAG, "Request URI: " + request.getURI());
            HttpResponse httpResponse = client.execute(request);
            HttpEntity entity = httpResponse.getEntity();
            String jsonText = EntityUtils.toString(entity, HTTP.UTF_8);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            Log.d(TAG, "Response code = " + statusCode);
            if (statusCode == HttpStatus.SC_OK)
                return new JSONObject(jsonText);
        }
        catch (ClientProtocolException e)
        {
        }
        catch (IOException e)
        {
        }
        catch (JSONException e)
        {
        }

        return null;
    }

    protected HttpGet getHttpGetObject(String absoluteUrl)
    {
        return new HttpGet(absoluteUrl);
    }

    protected String buildUri(String host, String path, Map<String, String> queryParams)
    {
        Builder uriBuilder = Uri.parse(host).buildUpon().appendPath(path);

        if (queryParams != null)
        {
            for (Map.Entry<String, String> entrySet : queryParams.entrySet())
                uriBuilder.appendQueryParameter(entrySet.getKey(), entrySet.getValue());
        }

        return uriBuilder.build().toString();
    }

}
