package cn.trinea.android.common.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import cn.trinea.android.common.dao.HttpCacheDao;
import cn.trinea.android.common.dao.impl.HttpCacheDaoImpl;
import cn.trinea.android.common.entity.HttpRequest;
import cn.trinea.android.common.entity.HttpResponse;
import cn.trinea.android.common.service.impl.SimpleCache;
import cn.trinea.android.common.util.HttpUtils;
import cn.trinea.android.common.util.SqliteUtils;
import cn.trinea.android.common.util.StringUtils;

/**
 * HttpCache
 * 
 * @author <a href="http://www.trinea.cn" target="_blank">Trinea</a> 2013-11-1
 */
public class HttpCache extends SimpleCache<String, HttpResponse> {

    private Context                   context;

    /** http memory cache **/
    private Map<String, HttpResponse> cache;
    /** dao to get data from http db cache **/
    private HttpCacheDao              httpCacheDaoImpl;

    public HttpCache(Context context){
        if (context == null) {
            throw new IllegalArgumentException("The context can not be null.");
        }
        this.context = context;
        cache = new ConcurrentHashMap<String, HttpResponse>();
        httpCacheDaoImpl = new HttpCacheDaoImpl(SqliteUtils.getInstance(context));
    }

    /**
     * @param context
     * @param type get httpResponse whose type is type into memory as primary cache to improve performance
     */
    public HttpCache(Context context, int type){
        this(context);
        initData(type);
    }

    /**
     * get httpResponse whose type is type into memory as primary cache to improve performance
     * 
     * @param type
     */
    private void initData(int type) {
        this.cache = httpCacheDaoImpl.getHttpResponsesByType(type);
        if (cache == null) {
            cache = new HashMap<String, HttpResponse>();
        }
    }

    public HttpResponse httpGet(HttpRequest request) {
        String url;
        if (request == null || StringUtils.isEmpty(url = request.getUrl())) {
            return null;
        }

        HttpResponse cacheResponse = getFromCache(url);
        return cacheResponse == null ? HttpUtils.httpGet(request) : cacheResponse;
    }

    public HttpResponse httpGet(String httpUrl) {
        HttpResponse cacheResponse = getFromCache(httpUrl);
        return cacheResponse == null ? HttpUtils.httpGet(httpUrl) : cacheResponse;
    }

    public String httpGetString(String httpUrl) {
        HttpResponse cacheResponse = getFromCache(httpUrl);
        return cacheResponse == null ? HttpUtils.httpGetString(httpUrl) : cacheResponse.getResponseBody();
    }

    /**
     * get from memory cache first, if not exist in memory cache, get from db
     * 
     * @param httpUrl
     * @return <ul>
     * <li>if neither exit in memory cache nor db, return null</li>
     * <li>if is expired, return null, otherwise return cache response</li>
     * </ul>
     */
    private HttpResponse getFromCache(String httpUrl) {
        if (StringUtils.isEmpty(httpUrl)) {
            return null;
        }

        HttpResponse cacheResponse = cache.get(httpUrl);
        if (cacheResponse == null) {
            cacheResponse = httpCacheDaoImpl.getHttpResponse(httpUrl);
        }
        return (cacheResponse == null || cacheResponse.isExpired()) ? null : cacheResponse;
    }
}
