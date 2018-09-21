



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.samples.youtube.cmdline.Auth;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentListResponse;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.common.collect.Lists;
import com.vdurmont.emoji.EmojiParser;


import org.apache.spark.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
/**
 * This sample creates and manages comments by:
 *
 * 1. Retrieving the top-level comments for a video via "commentThreads.list" method.
 * 2. Replying to a comment thread via "comments.insert" method.
 * 3. Retrieving comment replies via "comments.list" method.
 * 4. Updating an existing comment via "comments.update" method.
 * 5. Sets moderation status of an existing comment via "comments.setModerationStatus" method.
 * 6. Marking a comment as spam via "comments.markAsSpam" method.
 * 7. Deleting an existing comment via "comments.delete" method.
 *
 * @author Ibrahim Ulukaya
 */
public class CommentHandling {

    /**
     * Define a global instance of a YouTube object, which will be used to make
     * YouTube Data API requests.
     */
    private static YouTube youtube;

    /**
     * List, reply to comment threads; list, update, moderate, mark and delete
     * replies.
     *
     * @param args command line args (not used).
     */
    public static void main(String[] args) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account and requires requests to use an SSL connection.
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");

        try {
        	 BufferedWriter out = new BufferedWriter(new FileWriter("out.txt"));
       

           
            // Authorize the request.
            Credential credential = Auth.authorize(scopes, "commentthreads");

            // This object is used to make YouTube Data API requests.
          
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                    .setApplicationName("youtube-cmdline-commentthreads-sample").build();

            // Prompt the user for the ID of a video to comment on.
            // Retrieve the video ID that the user is commenting to.
            Scanner scan = new Scanner(System.in);
            System.out.println("1 : 단일 분석, 2: 비교분석");
            Integer option = scan.nextInt();
            if(option==2)
            {
            	System.out.println("상호명/상품명 입력");
            	String keyword = scan.next();
            	 Document doc = Jsoup.connect("https://www.youtube.com/results?q="+keyword).
            			 header("User-Agent", "Chrome").get();
         		
        		 Elements ele = doc.getElementsByTag("body");
        		 Element elem = ele.get(0).getElementById("results").getElementsByClass("section-list").get(0);
        		 Elements ele2 = elem.getElementsByClass("item-section").get(0).
        				 getElementsByClass("yt-lockup yt-lockup-tile yt-lockup-video vve-check clearfix");
        		 for(int i=0;i<ele2.size();i++) {
        			System.out.println(ele2.get(i).attr("data-context-item-id"));
        			BufferedWriter out_com = new BufferedWriter(new FileWriter(keyword+"_out"+i+".txt"));
        		       
        			String videoId = ele2.get(i).attr("data-context-item-id");
        			 String text = getText();
        	            System.out.println("You chose " + text + " to subscribe.");

        	            // All the available methods are used in sequence just for the sake
        	            // of an example.

        	            // Call the YouTube Data API's commentThreads.list method to
        	            // retrieve video comment threads.
        	            
        	            String nextToken=null;
        	            CommentThreadListResponse videoCommentsListResponse = youtube.commentThreads()
        	                    .list("snippet").setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 100).execute();
        	            
        	            do {
        	          
        	            nextToken = videoCommentsListResponse.getNextPageToken();
        	            
        	          
        	            List<CommentThread> videoComments = videoCommentsListResponse.getItems();

        	            if (videoComments.isEmpty()) {
        	                System.out.println("Can't get video comments.");
        	            } else {
        	                // Print information from the API response.
        	                System.out
        	                        .println("\n================== Returned Video Comments ==================\n");
        	                for (CommentThread videoComment : videoComments) {
        	                    CommentSnippet snippet = videoComment.getSnippet().getTopLevelComment()
        	                            .getSnippet();
        	                    System.out.println("  - Author: " + snippet.getAuthorDisplayName());
        	                    System.out.println("  - Comment: " + snippet.getTextDisplay());
        	                    String encode = URLEncoder.encode(snippet.getTextDisplay(), "UTF-8");
        	                    String result = EmojiParser.parseToAliases(snippet.getTextDisplay());
        	                    System.out.println(result);
        	                    out_com.write(result);
        	                    out_com.newLine();
        	                    System.out.println("\n-------------------------------------------------------------\n");

        	                }
        	                
        	               
        	                
        	                
        	            
        	            
        	            
        	            
        	                CommentThread firstComment = videoComments.get(0);

        	                // Will use this thread as parent to new reply.
        	                String parentId = firstComment.getId();

        	                // Create a comment snippet with text.
        	                CommentSnippet commentSnippet = new CommentSnippet();
        	                commentSnippet.setTextOriginal(text);
        	                commentSnippet.setParentId(parentId);

        	                // Create a comment with snippet.
        	                Comment comment = new Comment();
        	                comment.setSnippet(commentSnippet);

        	                // Call the YouTube Data API's comments.insert method to reply
        	                // to a comment.
        	                // (If the intention is to create a new top-level comment,
        	                // commentThreads.insert
        	                // method should be used instead.)
        	                Comment commentInsertResponse = youtube.comments().insert("snippet", comment)
        	                        .execute();

        	                // Print information from the API response.
        	                System.out
        	                        .println("\n================== Created Comment Reply ==================\n");
        	                CommentSnippet snippet = commentInsertResponse.getSnippet();
        	                System.out.println("  - Author: " + snippet.getAuthorDisplayName());
        	                System.out.println("  - Comment: " + snippet.getTextDisplay());
        	                System.out
        	                        .println("\n-------------------------------------------------------------\n");

        	                // Call the YouTube Data API's comments.list method to retrieve
        	                // existing comment
        	                // replies.
        	                CommentListResponse commentsListResponse = youtube.comments().list("snippet")
        	                        .setParentId(parentId).setTextFormat("plainText").execute();
        	                List<Comment> comments = commentsListResponse.getItems();

        	                if (comments.isEmpty()) {
        	                    System.out.println("Can't get comment replies.");
        	                } else {
        	                    // Print information from the API response.
        	                    System.out
        	                            .println("\n================== Returned Comment Replies ==================\n");
        	                    for (Comment commentReply : comments) {
        	                        snippet = commentReply.getSnippet();
        	                        System.out.println("  - Author: " + snippet.getAuthorDisplayName());
        	                        System.out.println("  - Comment: " + snippet.getTextDisplay());
        	                        System.out
        	                                .println("\n-------------------------------------------------------------\n");
        	                    }
        	                    Comment firstCommentReply = comments.get(0);
        	                    firstCommentReply.getSnippet().setTextOriginal("updated");
        	                    Comment commentUpdateResponse = youtube.comments()
        	                            .update("snippet", firstCommentReply).execute();
        	                    // Print information from the API response.
        	                    System.out
        	                            .println("\n================== Updated Video Comment ==================\n");
        	                    snippet = commentUpdateResponse.getSnippet();
        	                    System.out.println("  - Author: " + snippet.getAuthorDisplayName());
        	                    System.out.println("  - Comment: " + snippet.getTextDisplay());
        	                    System.out
        	                            .println("\n-------------------------------------------------------------\n");

        	                    // Call the YouTube Data API's comments.setModerationStatus
        	                    // method to set moderation
        	                    // status of an existing comment.
        	                    youtube.comments().setModerationStatus(firstCommentReply.getId(), "published");
        	                    System.out.println("  -  Changed comment status to published: "
        	                            + firstCommentReply.getId());

        	                    // Call the YouTube Data API's comments.markAsSpam method to
        	                    // mark an existing comment as spam.
        	                    youtube.comments().markAsSpam(firstCommentReply.getId());
        	                    System.out.println("  -  Marked comment as spam: " + firstCommentReply.getId());

        	                    // Call the YouTube Data API's comments.delete method to
        	                    // delete an existing comment.
        	                    youtube.comments().delete(firstCommentReply.getId());
        	                    System.out
        	                            .println("  -  Deleted comment as spam: " + firstCommentReply.getId());
        	                }
        	            }
        	            videoCommentsListResponse = youtube.commentThreads()
        	                    .list("snippet").setVideoId(videoId).setTextFormat("plainText").setPageToken(nextToken)
        	                    .setMaxResults((long) 100).execute();
        	            }
        	            while(nextToken!=null);
        	            
        	            out_com.close();

        		}
        				
            }
            else {
            String videoId = getVideoId();
            System.out.println("You chose " + videoId + " to subscribe.");

            // Prompt the user for the comment text.
            // Retrieve the text that the user is commenting.
            String text = getText();
            System.out.println("You chose " + text + " to subscribe.");

            // All the available methods are used in sequence just for the sake
            // of an example.

            // Call the YouTube Data API's commentThreads.list method to
            // retrieve video comment threads.
            
            String nextToken=null;
            CommentThreadListResponse videoCommentsListResponse = youtube.commentThreads()
                    .list("snippet").setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 100).execute();
            
            do {
          
            nextToken = videoCommentsListResponse.getNextPageToken();
            
          
            List<CommentThread> videoComments = videoCommentsListResponse.getItems();

            if (videoComments.isEmpty()) {
                System.out.println("Can't get video comments.");
            } else {
                // Print information from the API response.
                System.out
                        .println("\n================== Returned Video Comments ==================\n");
                for (CommentThread videoComment : videoComments) {
                    CommentSnippet snippet = videoComment.getSnippet().getTopLevelComment()
                            .getSnippet();
                    System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                    System.out.println("  - Comment: " + snippet.getTextDisplay());
                    String encode = URLEncoder.encode(snippet.getTextDisplay(), "UTF-8");
                    String result = EmojiParser.parseToAliases(snippet.getTextDisplay());
                    System.out.println(result);
                    out.write(result);
                    out.newLine();
                    System.out.println("\n-------------------------------------------------------------\n");

                }
                
               
                
                
            
            
            
            
                CommentThread firstComment = videoComments.get(0);

                // Will use this thread as parent to new reply.
                String parentId = firstComment.getId();

                // Create a comment snippet with text.
                CommentSnippet commentSnippet = new CommentSnippet();
                commentSnippet.setTextOriginal(text);
                commentSnippet.setParentId(parentId);

                // Create a comment with snippet.
                Comment comment = new Comment();
                comment.setSnippet(commentSnippet);

                // Call the YouTube Data API's comments.insert method to reply
                // to a comment.
                // (If the intention is to create a new top-level comment,
                // commentThreads.insert
                // method should be used instead.)
                Comment commentInsertResponse = youtube.comments().insert("snippet", comment)
                        .execute();

                // Print information from the API response.
                System.out
                        .println("\n================== Created Comment Reply ==================\n");
                CommentSnippet snippet = commentInsertResponse.getSnippet();
                System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                System.out.println("  - Comment: " + snippet.getTextDisplay());
                System.out
                        .println("\n-------------------------------------------------------------\n");

                // Call the YouTube Data API's comments.list method to retrieve
                // existing comment
                // replies.
                CommentListResponse commentsListResponse = youtube.comments().list("snippet")
                        .setParentId(parentId).setTextFormat("plainText").execute();
                List<Comment> comments = commentsListResponse.getItems();

                if (comments.isEmpty()) {
                    System.out.println("Can't get comment replies.");
                } else {
                    // Print information from the API response.
                    System.out
                            .println("\n================== Returned Comment Replies ==================\n");
                    for (Comment commentReply : comments) {
                        snippet = commentReply.getSnippet();
                        System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                        System.out.println("  - Comment: " + snippet.getTextDisplay());
                        System.out
                                .println("\n-------------------------------------------------------------\n");
                    }
                    Comment firstCommentReply = comments.get(0);
                    firstCommentReply.getSnippet().setTextOriginal("updated");
                    Comment commentUpdateResponse = youtube.comments()
                            .update("snippet", firstCommentReply).execute();
                    // Print information from the API response.
                    System.out
                            .println("\n================== Updated Video Comment ==================\n");
                    snippet = commentUpdateResponse.getSnippet();
                    System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                    System.out.println("  - Comment: " + snippet.getTextDisplay());
                    System.out
                            .println("\n-------------------------------------------------------------\n");

                    // Call the YouTube Data API's comments.setModerationStatus
                    // method to set moderation
                    // status of an existing comment.
                    youtube.comments().setModerationStatus(firstCommentReply.getId(), "published");
                    System.out.println("  -  Changed comment status to published: "
                            + firstCommentReply.getId());

                    // Call the YouTube Data API's comments.markAsSpam method to
                    // mark an existing comment as spam.
                    youtube.comments().markAsSpam(firstCommentReply.getId());
                    System.out.println("  -  Marked comment as spam: " + firstCommentReply.getId());

                    // Call the YouTube Data API's comments.delete method to
                    // delete an existing comment.
                    youtube.comments().delete(firstCommentReply.getId());
                    System.out
                            .println("  -  Deleted comment as spam: " + firstCommentReply.getId());
                }
            }
            videoCommentsListResponse = youtube.commentThreads()
                    .list("snippet").setVideoId(videoId).setTextFormat("plainText").setPageToken(nextToken)
                    .setMaxResults((long) 100).execute();
            }
            while(nextToken!=null);
            
            out.close();
            
            SparkConf conf = new SparkConf().setAppName("Line Count");
            JavaSparkContext ctx = new JavaSparkContext(conf);
            JavaRDD<String> textLoadRDD = ctx.textFile("C:\\Users\\세진\\eclipse-workspace\\youtube\\out.txt");
            System.out.println(textLoadRDD.count());
            
            
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode()
                    + " : " + e.getDetails().getMessage());
            e.printStackTrace();

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /*
     * Prompt the user to enter a video ID. Then return the ID.
     */
    private static String getVideoId() throws IOException {

        String url = "";
        String videoId = "";

        System.out.print("Please enter URL: ");
      //  BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        Scanner scan = new Scanner(System.in);
        url = scan.next();
        System.out.println(url);
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";

        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(url); 
        if (m.find()) {
            videoId = m.group();
       }
        System.out.println(m.group());
        videoId = m.group();
        return videoId;
    }

    /*
     * Prompt the user to enter text for a comment. Then return the text.
     */
    private static String getText() throws IOException {

        String text = "";

       
            // If nothing is entered, defaults to "YouTube For Developers."
            text = "YouTube For Developers.";
        
        return text;
    }
}

