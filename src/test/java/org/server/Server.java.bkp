/* ----------------------------------------------------------
   Server
   usage: java Server [RTSP listening port]
   ---------------------------------------------------------- */
package org.server;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

public class Server extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 5211838473915680157L;

	/*----------------------------------------------------------------
	 * UI-related variables
	 * ---------------------------------------------------------------*/
	JLabel label;

	/*----------------------------------------------------------------
	 * Video stream -related variables
	 * ---------------------------------------------------------------*/

	static int MJPEG_TYPE = 26;		// MJPEG video RTP payload type
	static int FRAME_PERIOD = 50;	// video frame period in ms
	static int VIDEO_LENGTH = 500;	// video length in frames

	VideoStream videoStream;				// stream object used to access video frames
	byte[] sendImageBuffer;				// buffer for images to be sent to client
	int imageCounter = 0;			// currently transmitted image number/counter

	/* A delay required to send images over the wire.
	 * Ideally equal to the video file frame rate but may be adjusted in case of congestion.*/
	int sendDelay;
	Timer sendTimer;					// timer used to send images at video frame rate

	/*----------------------------------------------------------------
	 * Real-Time Transfer Protocol (RTP) -related variables.
	 * ---------------------------------------------------------------*/

	DatagramSocket rtpSocket;	// a socket to send and receive UDP packets
	DatagramPacket sendDp; 		// a UDP packet containing the video frames
	InetAddress clientIp;
	int rtpDestPort = 0;		//destination port for RTP packets  (provided by the RTSP Client)
	int rtspDestPort = 13569;

	/*----------------------------------------------------------------
	 * Real-Time Streaming Protocol (RTSP) -related variables.
	 * ---------------------------------------------------------------*/

	static int RTSP_ID = 123456; // RTSP session ID
	static int RTSP_PORT = 13569;

	// RTSP states
	final static int INIT = 0;
	final static int READY = 1;
	final static int PLAYING = 2;

	// RTSP message types
	final static int SETUP = 3;
	final static int PLAY = 4;
	final static int PAUSE = 5;
	final static int TEARDOWN = 6;
	final static int DESCRIBE = 7;

	/* A socket used to send/receive RTSP messages */
	Socket rtspSocket; //

	// input/output stream filters
	static BufferedReader rtspBufferedReader;
	static BufferedWriter rtspBufferedWriter;

	static int state;				/* RTSP Server states: INIT, READY, PLAY */
	int rtspSeqNum = 0;				/* RTSP messages sequence number, within a session */
	static String videoFileName;	// video file name requested from the client

	/*----------------------------------------------------------------
	 * Real-Time Control Protocol (RTCP) -related variables.
	 ----------------------------------------------------------------*/

	static int RTCP_RCV_PORT = 19001;	// client's RTCP packets receiving port
	static int RTCP_PERIOD = 400;		// control events check frequency
	final static String CRLF = "\r\n";

	DatagramSocket rtcpSocket;
	RtcpReceiver rtcpReceiver;
	int congestionLevel;

	//Performance optimization and Congestion control
	ImageEncoder imageEncoder;
	CongestionController congestionController;

	/**----------------------------------------------------------------
	 * Constructor.
	 * ----------------------------------------------------------------*/
	public Server()
	{
		// initialize frame, TODO: replace with JavaFX
		super("Server");

		// initialize RTP sending Timer
		sendDelay = FRAME_PERIOD;
		sendTimer = new Timer(sendDelay, this);
		sendTimer.setInitialDelay(0);
		sendTimer.setCoalesce(true);

		// initialize congestion controller
		congestionController = new CongestionController(600);

		// allocate memory for sending image buffer
		sendImageBuffer = new byte[20000];

		// main window handler (close)
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e)
			{
				// stop the timer and exit (refactor to use FxTimer instead)
				sendTimer.stop();
				rtcpReceiver.stopRcv();
				System.exit(0);
			}
		});

		// initialize RTCP packet receiver
		rtcpReceiver = new RtcpReceiver(RTCP_PERIOD);

		// GUI
		label = new JLabel("Send frame #		", JLabel.CENTER);
		getContentPane().add(label, BorderLayout.CENTER);

		// Video encoding and quality
		imageEncoder = new ImageEncoder(0.8f);
	}

	/**----------------------------------------------------------------
	 * Executor.
	 * ----------------------------------------------------------------*/
	public static void main(String argv[]) throws Exception
	{
		// create a Server object
		Server theServer = new Server();

		// show GUI:
		theServer.pack();
		theServer.setVisible(true);

		// Initiate TCP connection with the client
		ServerSocket listenSocket = new ServerSocket(theServer.rtspDestPort);
		theServer.rtspSocket = listenSocket.accept();
		listenSocket.close();

		// Get Client IP address
		theServer.clientIp = theServer.rtspSocket.getInetAddress();

		// Initiate RTSPstate
		state = INIT;

		//Set input and output stream filters:
		rtspBufferedReader = new BufferedReader(new InputStreamReader(theServer.rtspSocket.getInputStream()) );
		rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(theServer.rtspSocket.getOutputStream()) );

		/* Setup RTSP communication (blocking) by parsing and interpreting client's request.
		 * When "SETUP" is received, update connection state, send confirmation message,
		 * initialize video stream, as well as RTP and RTCP sockets */
		int requestType;
		boolean done = false;
		while(!done)
		{
			requestType = theServer.parseRtspRequest(); //blocking
			if (requestType == SETUP)
			{
				done = true;
				state = READY;
				System.out.println("New RTSP state: READY");

				theServer.sendRtspResponse();
				theServer.videoStream = new VideoStream(videoFileName);
				theServer.rtpSocket = new DatagramSocket();
				theServer.rtcpSocket = new DatagramSocket(RTCP_RCV_PORT);
			}
		}

		/* Loop to handle RTSP requests once the setup is complete. */
		while(true)
		{
			requestType = theServer.parseRtspRequest(); //blocking
			if ((requestType == PLAY) && (state == READY))
			{
				theServer.sendRtspResponse();
				theServer.sendTimer.start();
				theServer.rtcpReceiver.startRcv();
				state = PLAYING;
				System.out.println("New RTSP state: PLAYING");
			}
			else if ((requestType == PAUSE) && (state == PLAYING))
			{
				theServer.sendRtspResponse();
				theServer.sendTimer.stop();
				theServer.rtcpReceiver.stopRcv();
				state = READY;
				System.out.println("New RTSP state: READY");
			}
			else if (requestType == TEARDOWN)
			{
				theServer.sendRtspResponse();
				theServer.sendTimer.stop();
				theServer.rtcpReceiver.stopRcv();
				theServer.rtspSocket.close();
				theServer.rtpSocket.close();
				System.exit(0);
			}
			else if (requestType == DESCRIBE)
			{
				System.out.println("Received DESCRIBE request");
				theServer.sendRtspDescribe();
			}
		}
	}

	//------------------------
	// Handler for timer
	//------------------------
	@Override
	public void actionPerformed(ActionEvent e) {
		byte[] frame;

		//if the current image nb is less than the length of the video
		if (imageCounter < VIDEO_LENGTH) {
			//update current imagenb
			imageCounter++;

			try {
				//get next frame to send from the video, as well as its size
				int image_length = videoStream.getNextFrame(sendImageBuffer);

				//adjust quality of the image if there is congestion detected
				if (congestionLevel > 0) {
					imageEncoder.setCompressionQuality(1.0f - (congestionLevel * 0.2f));
					frame = imageEncoder.compress(Arrays.copyOfRange(sendImageBuffer, 0, image_length));
					image_length = frame.length;
					System.arraycopy(frame, 0, sendImageBuffer, 0, image_length);
				}

				//Builds an RTPpacket object containing the frame
				RtpPacket rtp_packet = new RtpPacket(MJPEG_TYPE, imageCounter, imageCounter*FRAME_PERIOD, sendImageBuffer, image_length);

				//get to total length of the full rtp packet to send
				int packet_length = rtp_packet.getlength();

				//retrieve the packet bitstream and store it in an array of bytes
				byte[] packet_bits = new byte[packet_length];
				rtp_packet.getpacket(packet_bits);

				//send the packet as a DatagramPacket over the UDP socket
				sendDp = new DatagramPacket(packet_bits, packet_length, clientIp, rtpDestPort);
				rtpSocket.send(sendDp);

				System.out.println("Send frame #" + imageCounter + ", Frame size: " + image_length + " (" + sendImageBuffer.length + ")");
				//print the header bitstream
				rtp_packet.printheader();

				//update GUI
				label.setText("Send frame #" + imageCounter);
			}
			catch(Exception ex) {
				System.out.println("Exception caught: "+ex);
				System.exit(0);
			}
		}
		else {
			//if we have reached the end of the video file, stop the timer
			sendTimer.stop();
			rtcpReceiver.stopRcv();
		}
	}

	/**--------------------------------------------------------------------------------------------
	 * Controls RTP sending rate based on traffic statistics.
	 * --------------------------------------------------------------------------------------------*/
	class CongestionController implements ActionListener
	{
		private Timer ccTimer;
		int interval;   // interval to check traffic stats
		int prevLevel;  // previously sampled congestion level

		public CongestionController(int interval)
		{
			this.interval = interval;
			ccTimer = new Timer(interval, this);
			ccTimer.start();
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			//adjust the send rate
			if (prevLevel != congestionLevel)
			{
				sendDelay = FRAME_PERIOD + (congestionLevel * (int)(FRAME_PERIOD * 0.1));
				sendTimer.setDelay(sendDelay);
				prevLevel = congestionLevel;
				System.out.println("Send delay changed to: " + sendDelay);
			}
		}
	}

	/**--------------------------------------------------------------------------------------------
	 * Listener for RTCP packets sent by client.
	 * --------------------------------------------------------------------------------------------*/
	class RtcpReceiver implements ActionListener
	{
		private Timer rtcpTimer;
		private byte[] rtcpBuffer;
		int interval;

		public RtcpReceiver(int interval) {
			//set timer with interval for receiving packets
			this.interval = interval;
			rtcpTimer = new Timer(interval, this);
			rtcpTimer.setInitialDelay(0);
			rtcpTimer.setCoalesce(true);

			//allocate buffer for receiving RTCP packets
			rtcpBuffer = new byte[512];
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			//Construct a DatagramPacket to receive data from the UDP socket
			DatagramPacket dp = new DatagramPacket(rtcpBuffer, rtcpBuffer.length);
			float fractionLost;

			try {
				rtcpSocket.receive(dp);   // Blocking
				RtcpPacket rtcpPkt = new RtcpPacket(dp.getData(), dp.getLength());
				System.out.println("[RTCP] " + rtcpPkt);

				//set congestion level between 0 to 4
				fractionLost = rtcpPkt.fractionLost;
				if ((fractionLost >= 0) && (fractionLost <= 0.01)) {
					congestionLevel = 0;	//less than 0.01 assume negligible
				}
				else if ((fractionLost > 0.01) && (fractionLost <= 0.25)) {
					congestionLevel = 1;
				}
				else if ((fractionLost > 0.25) && (fractionLost <= 0.5)) {
					congestionLevel = 2;
				}
				else if ((fractionLost > 0.5) && (fractionLost <= 0.75)) {
					congestionLevel = 3;
				}
				else {
					congestionLevel = 4;
				}
			}
			catch (InterruptedIOException iioe) {
				System.out.println("Nothing to read");
			}
			catch (IOException ioe) {
				System.out.println("Exception caught: "+ioe);
			}
		}

		public void startRcv() {
			rtcpTimer.start();
		}

		public void stopRcv() {
			rtcpTimer.stop();
		}
	}

	//------------------------------------
	//Parse RTSP Request
	//------------------------------------
	private int parseRtspRequest()
	{
		int request_type = -1;
		try {
			//parse request line and extract the request_type:
			String RequestLine = rtspBufferedReader.readLine();
			System.out.println("RTSP Server - Received from Client:");
			System.out.println(RequestLine);

			StringTokenizer tokens = new StringTokenizer(RequestLine);
			String request_type_string = tokens.nextToken();

			//convert to request_type structure:
			if ((new String(request_type_string)).compareTo("SETUP") == 0)
				request_type = SETUP;
			else if ((new String(request_type_string)).compareTo("PLAY") == 0)
				request_type = PLAY;
			else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
				request_type = PAUSE;
			else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
				request_type = TEARDOWN;
			else if ((new String(request_type_string)).compareTo("DESCRIBE") == 0)
				request_type = DESCRIBE;

			if (request_type == SETUP) {
				//extract VideoFileName from RequestLine
				videoFileName = tokens.nextToken();
			}

			//parse the SeqNumLine and extract CSeq field
			String SeqNumLine = rtspBufferedReader.readLine();
			System.out.println(SeqNumLine);
			tokens = new StringTokenizer(SeqNumLine);
			tokens.nextToken();
			rtspSeqNum = Integer.parseInt(tokens.nextToken());

			//get LastLine
			String LastLine = rtspBufferedReader.readLine();
			System.out.println(LastLine);

			tokens = new StringTokenizer(LastLine);
			if (request_type == SETUP) {
				//extract RTP_dest_port from LastLine
				for (int i=0; i<3; i++)
					tokens.nextToken(); //skip unused stuff
				rtpDestPort = Integer.parseInt(tokens.nextToken());
			}
			else if (request_type == DESCRIBE) {
				tokens.nextToken();
				String describeDataType = tokens.nextToken();
			}
			else {
				//otherwise LastLine will be the SessionId line
				tokens.nextToken(); //skip Session:
				RTSP_ID = Integer.parseInt(tokens.nextToken());
			}
		} catch(Exception ex) {
			System.out.println("Exception caught: "+ex);
			System.exit(0);
		}

		return(request_type);
	}

	// Creates a DESCRIBE response string in SDP format for current media
	private String describe() {
		StringWriter writer1 = new StringWriter();
		StringWriter writer2 = new StringWriter();

		// Write the body first so we can get the size later
		writer2.write("v=0" + CRLF);
		writer2.write("m=video " + rtspDestPort + " RTP/AVP " + MJPEG_TYPE + CRLF);
		writer2.write("a=control:streamid=" + RTSP_ID + CRLF);
		writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
		String body = writer2.toString();

		writer1.write("Content-Base: " + videoFileName + CRLF);
		writer1.write("Content-Type: " + "application/sdp" + CRLF);
		writer1.write("Content-Length: " + body.length() + CRLF);
		writer1.write(body);

		return writer1.toString();
	}

	//------------------------------------
	//Send RTSP Response
	//------------------------------------
	private void sendRtspResponse() {
		try {
			rtspBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
			rtspBufferedWriter.write("CSeq: " + rtspSeqNum + CRLF);
			rtspBufferedWriter.write("Session: " + RTSP_ID + CRLF);
			rtspBufferedWriter.flush();
			System.out.println("RTSP Server - Sent response to Client.");
		} catch(Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
	}

	private void sendRtspDescribe() {
		String des = describe();
		try {
			rtspBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
			rtspBufferedWriter.write("CSeq: "+rtspSeqNum+CRLF);
			rtspBufferedWriter.write(des);
			rtspBufferedWriter.flush();
			System.out.println("RTSP Server - Sent response to Client.");
		} catch(Exception ex) {
			System.out.println("Exception caught: "+ex);
			System.exit(0);
		}
	}
}