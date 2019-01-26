package org.learning.javaconcurrency.executor;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.learning.javaconcurrency.CustomThreads;
import org.learning.javaconcurrency.Event;
import org.learning.javaconcurrency.service.JsonService;
import org.learning.javaconcurrency.service.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by vkasiviswanathan on 1/6/19.
 */
@Component
public class AsyncExecutorService {

	private static final Logger LOG = LoggerFactory.getLogger(AsyncExecutorService.class);

	public String getAsyncResponse(int ioPoolSize, boolean fixedWorkerThreadForNonIoTasks) {

		Event event = new Event();

		try {
			if (ioPoolSize == 0) {
				useDefaultPoolForIoAndNonIoTasks(event);
			} else if (fixedWorkerThreadForNonIoTasks) {
				useThreadPoolForIoTasksAndWorkerThreadForNonIoTasks(ioPoolSize, event);
			} else {
				useCustomThreadPoolForIoAndNonIoTasks(ioPoolSize, event);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// blocking call from the thread which handles Http Request
		while (event.response == null) {
		}
		LOG.info("Sending response from AsyncExecutorService : " + Thread.currentThread().getName());
		return event.response;
	}

	private void useDefaultPoolForIoAndNonIoTasks(Event event) {

		int userId = new Random().nextInt(10) + 1;
		CompletableFuture<String> postsFuture = CompletableFuture.supplyAsync(JsonService::getPosts);
		CompletableFuture<String> commentsFuture = CompletableFuture.supplyAsync(JsonService::getComments);
		CompletableFuture<String> albumsFuture = CompletableFuture.supplyAsync(JsonService::getAlbums);
		CompletableFuture<String> photosFuture = CompletableFuture.supplyAsync(JsonService::getPhotos);

		CompletableFuture<String> postsAndCommentsFuture = postsFuture.thenCombineAsync(commentsFuture,
				(posts, comments) -> ResponseUtil.getPostsAndCommentsOfRandomUser(userId, posts, comments));

		CompletableFuture<String> albumsAndPhotosFuture = albumsFuture.thenCombineAsync(photosFuture,
				(albums, photos) -> ResponseUtil.getAlbumsAndPhotosOfRandomUser(userId, albums, photos));

		postsAndCommentsFuture.thenAcceptBothAsync(albumsAndPhotosFuture, (s1, s2) -> {
			LOG.info("Building Async Response in Thread " + Thread.currentThread().getName());
			String response = s1 + s2;
			event.response = response;
		});

	}

	private void useThreadPoolForIoTasksAndWorkerThreadForNonIoTasks(int ioPoolSize, Event event) {

		int userId = new Random().nextInt(10) + 1;
		ExecutorService ioExecutorService = CustomThreads.getExecutorService(ioPoolSize);
		CompletableFuture<String> postsFuture = CompletableFuture.supplyAsync(JsonService::getPosts, ioExecutorService);
		CompletableFuture<String> commentsFuture = CompletableFuture.supplyAsync(JsonService::getComments,
				ioExecutorService);
		CompletableFuture<String> albumsFuture = CompletableFuture.supplyAsync(JsonService::getAlbums,
				ioExecutorService);
		CompletableFuture<String> photosFuture = CompletableFuture.supplyAsync(JsonService::getPhotos,
				ioExecutorService);

		CompletableFuture<String> postsAndCommentsFuture = postsFuture.thenCombineAsync(commentsFuture,
				(posts, comments) -> ResponseUtil.getPostsAndCommentsOfRandomUser(userId, posts, comments),
				CustomThreads.EXECUTOR_SERVICE_WORKER_1);

		CompletableFuture<String> albumsAndPhotosFuture = albumsFuture.thenCombineAsync(photosFuture,
				(albums, photos) -> ResponseUtil.getAlbumsAndPhotosOfRandomUser(userId, albums, photos),
				CustomThreads.EXECUTOR_SERVICE_WORKER_2);

		postsAndCommentsFuture.thenAcceptBothAsync(albumsAndPhotosFuture, (s1, s2) -> {
			LOG.info("Building Async Response in Thread " + Thread.currentThread().getName());
			String response = s1 + s2;
			event.response = response;
		}, CustomThreads.EXECUTOR_SERVICE_WORKER_1);

	}

	private void useCustomThreadPoolForIoAndNonIoTasks(int ioPoolSize, Event event) {

		int userId = new Random().nextInt(10) + 1;
		ExecutorService ioExecutorService = CustomThreads.getExecutorService(ioPoolSize);
		CompletableFuture<String> postsFuture = CompletableFuture.supplyAsync(JsonService::getPosts, ioExecutorService);
		CompletableFuture<String> commentsFuture = CompletableFuture.supplyAsync(JsonService::getComments,
				ioExecutorService);
		CompletableFuture<String> albumsFuture = CompletableFuture.supplyAsync(JsonService::getAlbums,
				ioExecutorService);
		CompletableFuture<String> photosFuture = CompletableFuture.supplyAsync(JsonService::getPhotos,
				ioExecutorService);

		CompletableFuture<String> postsAndCommentsFuture = postsFuture.thenCombineAsync(commentsFuture,
				(posts, comments) -> ResponseUtil.getPostsAndCommentsOfRandomUser(userId, posts, comments),
				ioExecutorService);

		CompletableFuture<String> albumsAndPhotosFuture = albumsFuture.thenCombineAsync(photosFuture,
				(albums, photos) -> ResponseUtil.getAlbumsAndPhotosOfRandomUser(userId, albums, photos),
				ioExecutorService);

		postsAndCommentsFuture.thenAcceptBothAsync(albumsAndPhotosFuture, (s1, s2) -> {
			LOG.info("Building Async Response in Thread " + Thread.currentThread().getName());
			String response = s1 + s2;
			event.response = response;
		}, ioExecutorService);

	}
}
