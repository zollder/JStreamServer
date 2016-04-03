package org.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**----------------------------------------------------------------------------------------------------------------
 * Sets up RTSP communication, performs message exchange routines and updates connection state.
 * ----------------------------------------------------------------------------------------------------------------*/
public class CommunicationService extends Service<Server>
{
	static String videoFileName = "movie.Mjpeg";	// video file name requested from the client

	private final Server server;

	public CommunicationService(Server serverInstance)
	{
		this.server = serverInstance;
	}

	@Override
	protected Task<Server> createTask()
	{
		Task<Server> task = new Task<Server>() {
			@Override
			protected Server call() throws Exception
			{
				// Initiate TCP connection with the client (blocking)
				ServerSocket listenSocket = new ServerSocket(server.rtspDestPort);
				server.rtspSocket = listenSocket.accept();
				listenSocket.close();

				// Get Client IP address
				server.clientIp = server.rtspSocket.getInetAddress();

				// Initiate RTSPstate
				Server.state = Server.INIT;

				//Set input and output stream filters:
				Server.rtspBufferedReader = new BufferedReader(new InputStreamReader(server.rtspSocket.getInputStream()) );
				Server.rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(server.rtspSocket.getOutputStream()) );

				/* Setup RTSP communication (blocking) by parsing and interpreting client's request.
				 * When "SETUP" is received, update connection state, send confirmation message,
				 * initialize video stream, as well as RTP and RTCP sockets */
				int requestType;
				boolean done = false;
				while(!done)
				{
					requestType = server.parseRtspRequest(); //blocking
					if (requestType == Server.SETUP)
					{
						done = true;
						Server.state =Server.READY;
						System.out.println("New RTSP state: READY");

						server.sendRtspResponse();
						server.videoStream = new VideoStream(videoFileName);
						server.rtpSocket = new DatagramSocket();
						server.rtcpSocket = new DatagramSocket(Server.RTCP_RCV_PORT);
					}
				}

				/* Loop to handle RTSP requests once the setup is complete. */
				while(true)
				{
					requestType = server.parseRtspRequest(); //blocking
					if ((requestType == Server.PLAY) && (Server.state == Server.READY)) {
						play();
					}
					else if ((requestType == Server.PAUSE) && (Server.state == Server.PLAYING)) {
						pause();
					}
					else if (requestType == Server.TEARDOWN) {
						tearDown();
					}
					else if (requestType == Server.DESCRIBE) {
						System.out.println("Received DESCRIBE request");
						server.sendRtspDescribe();
					}
				}
			}
        };

        return task;
	}

	private void play()
	{
		server.sendRtspResponse();
		server.sendTimer.start();
		server.rtcpReceiver.startRcv();
		Server.state = Server.PLAYING;
		System.out.println("New RTSP state: PLAYING");
	}

	private void pause()
	{
		server.sendRtspResponse();
		server.sendTimer.stop();
		server.rtcpReceiver.stopRcv();
		Server.state = Server.READY;
		System.out.println("New RTSP state: READY");
	}

	private void tearDown() throws IOException
	{
		System.out.println("DESTROYING ...");
		server.sendRtspResponse();
		server.sendTimer.stop();
		server.rtcpReceiver.stopRcv();
		server.rtspSocket.close();
		server.rtpSocket.close();
		System.exit(0);
	}
}