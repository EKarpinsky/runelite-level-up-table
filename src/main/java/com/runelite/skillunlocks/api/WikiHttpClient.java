package com.runelite.skillunlocks.api;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated HTTP client for OSRS Wiki API operations.
 * Handles HTTP communication, rate limiting, and request building.
 * Follows RuneLite patterns for injected HTTP services.
 */
@Slf4j
public class WikiHttpClient
{
	private static final String WIKI_API_URL = "https://oldschool.runescape.wiki/api.php";
	private static final String USER_AGENT = "RuneLite Skill Unlocks Plugin";
	
	// Rate limiting configuration
	private static final long MIN_REQUEST_INTERVAL_MS = 1000; // 1 request per second
	private static final int CONNECT_TIMEOUT_SECONDS = 30;
	private static final int READ_TIMEOUT_SECONDS = 30;
	
	private final OkHttpClient httpClient;
	private long lastRequestTime = 0;
	
	public WikiHttpClient(OkHttpClient httpClient)
	{
		// Create a custom configured client for wiki operations
		this.httpClient = httpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build();
	}
	
	/**
	 * Fetches wiki page content with proper rate limiting.
	 * 
	 * @param pageName The wiki page name to fetch
	 * @return The wiki page content in JSON format, or null if failed
	 * @throws IOException if the request fails
	 */
	public synchronized String fetchWikiPage(String pageName) throws IOException
	{
		enforceRateLimit();
		
		String encodedPageName = URLEncoder.encode(pageName, StandardCharsets.UTF_8);
		String url = buildApiUrl(encodedPageName);
		
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();
		
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				log.error("Failed to fetch wiki page: {} - HTTP {}", pageName, response.code());
				return null;
			}
			
			if (response.body() == null)
			{
				log.error("Received null response body for page: {}", pageName);
				return null;
			}
			
			return response.body().string();
		}
	}
	
	/**
	 * Enforces rate limiting to avoid overwhelming the wiki API.
	 * Blocks the thread if necessary to maintain the minimum request interval.
	 */
	private void enforceRateLimit()
	{
		long currentTime = System.currentTimeMillis();
		long timeSinceLastRequest = currentTime - lastRequestTime;
		
		if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS)
		{
			try
			{
				long sleepTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest;
				log.debug("Rate limiting: sleeping for {}ms", sleepTime);
				Thread.sleep(sleepTime);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				log.warn("Rate limiting sleep interrupted", e);
			}
		}
		
		lastRequestTime = System.currentTimeMillis();
	}
	
	/**
	 * Builds the wiki API URL for a given page.
	 * 
	 * @param encodedPageName The URL-encoded page name
	 * @return The complete API URL
	 */
	private String buildApiUrl(String encodedPageName)
	{
		return String.format(
			"%s?action=query&prop=revisions&titles=%s&rvslots=*&rvprop=content&format=json",
			WIKI_API_URL, 
			encodedPageName
		);
	}
	
}