/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.livestream;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.video.livestream.v1.Channel;
import com.google.cloud.video.livestream.v1.DeleteChannelRequest;
import com.google.cloud.video.livestream.v1.DeleteEventRequest;
import com.google.cloud.video.livestream.v1.DeleteInputRequest;
import com.google.cloud.video.livestream.v1.Event;
import com.google.cloud.video.livestream.v1.Input;
import com.google.cloud.video.livestream.v1.ListChannelsRequest;
import com.google.cloud.video.livestream.v1.ListEventsRequest;
import com.google.cloud.video.livestream.v1.ListInputsRequest;
import com.google.cloud.video.livestream.v1.LivestreamServiceClient;
import com.google.cloud.video.livestream.v1.LocationName;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestUtils {

  private static final int DELETION_THRESHOLD_TIME_HOURS_IN_SECONDS = 10800; // 3 hours

  public static void cleanAllStale(String projectId, String location) {
    cleanStaleChannels(projectId, location);
    cleanStaleInputs(projectId, location);
  }

  public static void cleanStaleInputs(String projectId, String location) {
    try (LivestreamServiceClient livestreamServiceClient = LivestreamServiceClient.create()) {
      var listInputsRequest =
          ListInputsRequest.newBuilder()
              .setParent(LocationName.of(projectId, location).toString())
              .build();

      LivestreamServiceClient.ListInputsPagedResponse response =
          livestreamServiceClient.listInputs(listInputsRequest);

      for (Input input : response.iterateAll()) {
        if (input.getCreateTime().getSeconds()
            < Instant.now().getEpochSecond() - DELETION_THRESHOLD_TIME_HOURS_IN_SECONDS) {
          var deleteInputRequest = DeleteInputRequest.newBuilder().setName(input.getName()).build();
          livestreamServiceClient.deleteInputAsync(deleteInputRequest).get(10, TimeUnit.MINUTES);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (NotFoundException | InterruptedException | ExecutionException | TimeoutException e) {
      e.printStackTrace();
    }
  }

  public static void cleanStaleChannels(String projectId, String location) {
    try (LivestreamServiceClient livestreamServiceClient = LivestreamServiceClient.create()) {
      var listChannelsRequest =
          ListChannelsRequest.newBuilder()
              .setParent(LocationName.of(projectId, location).toString())
              .build();

      LivestreamServiceClient.ListChannelsPagedResponse response =
          livestreamServiceClient.listChannels(listChannelsRequest);

      for (Channel channel : response.iterateAll()) {
        if (channel.getCreateTime().getSeconds()
            < Instant.now().getEpochSecond() - DELETION_THRESHOLD_TIME_HOURS_IN_SECONDS) {
          // Stop the channel
          try {
            livestreamServiceClient.stopChannelAsync(channel.getName()).get(10, TimeUnit.MINUTES);
          } catch (ExecutionException e) {
            // Ignore error if the channel isn't stopped or the stop operation times out.
            e.printStackTrace();
          } catch (NotFoundException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            continue;
          }
          // Delete the channel events
          var listEventsRequest =
              ListEventsRequest.newBuilder().setParent(channel.getName()).build();

          LivestreamServiceClient.ListEventsPagedResponse eventsResponse =
              livestreamServiceClient.listEvents(listEventsRequest);

          for (Event event : eventsResponse.iterateAll()) {
            var deleteEventRequest =
                DeleteEventRequest.newBuilder().setName(event.getName()).build();

            livestreamServiceClient.deleteEvent(deleteEventRequest);
          }
          // Delete the channel
          var deleteChannelRequest =
              DeleteChannelRequest.newBuilder().setName(channel.getName()).build();

          livestreamServiceClient
              .deleteChannelAsync(deleteChannelRequest)
              .get(10, TimeUnit.MINUTES);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (NotFoundException | InterruptedException | ExecutionException | TimeoutException e) {
      e.printStackTrace();
    }
  }
}
