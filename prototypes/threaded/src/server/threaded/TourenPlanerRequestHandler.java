/**
 * $$\\ToureNPlaner\\$$
 */

package server.threaded;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import computecore.ComputeCore;
import computecore.ComputeRequest;

/**
 * @author Christoph Haag, Peter Vollmer
 * 
 */
public class TourenPlanerRequestHandler implements HttpRequestHandler {

	// TODO: may be useful
	private String username = null;
	private String password = null;

	@Override
	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {
		this.response = response;
		this.request = request;
		LoggerStub.debugMsg(" RH: TourenPlaner Requesthandler called");

		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);

		if (method.equals("HEAD")) {
			handleHEAD();
		} else if (method.equals("POST")) {
			// direct connections not from java script XSS client
			if (auth((HttpEntityEnclosingRequest) request)) {
				LoggerStub
						.debugMsg(" RH: User Successfully Authenticated; Username: "
								+ username + "; Password: " + password);
				handlePOST();
			} else {
				reply(accessDenied);
				LoggerStub
						.debugMsg(" RH: User authentication FAILED; Username: "
								+ username + "; Password: " + password);
			}
		} else {
			// TODO: reply method not supported. Does apache http do this
			// automatically?
			reply(generalError);
			throw new MethodNotSupportedException(method
					+ " method not supported");
		}
	}

	private void handlePOST() {
		// TODO Auto-generated method stub
		String URI = request.getRequestLine().getUri();
		LoggerStub.debugMsg(" RH: Received request with URI: " + URI);

		// no idea what this does
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request)
					.getEntity();

			algName = URI.split("\\?")[0].substring(1);
			// parse the json that is hopefully in the input stream
			JSONObject requestJSON = null;
			/** JSONParser in here for threading **/
			final JSONParser parser = new JSONParser();
			try {
				byte[] entityContent = null;
				entityContent = EntityUtils.toByteArray(entity);
				// TODO: avoid useless casting of entitycontent
				requestJSON = (JSONObject) parser.parse(new String(
						entityContent));
			} catch (ParseException e) {
				LoggerStub.errorLog("JSON Parse error: " + e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				LoggerStub
						.errorLog(" RH: Something happened while converting the input stream to byte[]");
				e.printStackTrace();
			}

			LoggerStub.debugMsg(" RH: requested JSON: "
					+ requestJSON.toJSONString());

			// with valid Json this should be safe I guess and is valid
			// because the parser would have failed otherwise
			@SuppressWarnings("unchecked")
			Map<String, Object> objmap = requestJSON;

			LoggerStub.debugMsg(" RH: Algname: " + algName);

			// Create ComputeRequest and commit to workqueue
			final ComputeRequest req = new ComputeRequest(algName, objmap);
			boolean sucess = comCore.submit(req);
			if (!sucess) {
				// TODO: send server overload message
				reply(generalError);
				return;
			}

			try {
				// semaphore is signaled when computation is complete
				req.getWaitComputation().acquire();
			} catch (InterruptedException e) {
				// TODO if server is interrupted, shut down everything here
				LoggerStub
						.debugMsg("Interruption of Requesthandler. Shutting down...");
				return;
			}

			final String result = req.getResultObject().toString();
			LoggerStub
					.debugMsg(" RH: computation complete, sending this result to the client: "
							+ result);

			EntityTemplate body = new EntityTemplate(new ContentProducer() {
				@Override
				public void writeTo(OutputStream outstream) throws IOException {
					OutputStreamWriter writer = new OutputStreamWriter(
							outstream, "UTF-8");
					writer.write(result);
					writer.flush();
				}
			});
			body.setContentType("text/html; charset=UTF-8");
			response.setEntity(body);
		}
	}

	private void handleHEAD() {
		// TODO Auto-generated method stub

	}

	public TourenPlanerRequestHandler(ComputeCore comCore) {
		// no idea why Object's constructor is called
		super();
		this.comCore = comCore;
	}

	/**
	 * Authenticats the request in the ChannelBuffer content with the parameters
	 * given in params see: @link
	 * https://gerbera.informatik.uni-stuttgart.de/projects
	 * /server/wiki/Authentifizierung for a detailed explanation
	 * 
	 * @param params
	 * @param content
	 * @return
	 */
	private boolean auth(HttpEntityEnclosingRequest request) {
		boolean authentic = false;

		String userandpwdecoded = null;

		// decode userandpw
		try {
			String userandpwencoded = request.getFirstHeader("Authorization")
					.getValue();
			if (userandpwencoded == null) {
				return false;
			}
			// http headers are supposed to be ascii
			byte[] decoded = null;
			decoded = Base64.decodeBase64(userandpwencoded
					.substring(userandpwencoded.lastIndexOf(' ')).trim()
					.getBytes("US-ASCII"));
			userandpwdecoded = new String(decoded, "UTF-8").trim();
		} catch (UnsupportedEncodingException e) {
			// would be strange
			e.printStackTrace();
		}

		// TODO: may be useful
		username = userandpwdecoded.split(":")[0];
		password = userandpwdecoded.split(":")[1];

		// TODO: real user and password
		if (userandpwdecoded.equals("FooUser:FooPassword")) {
			authentic = true;
		}
		LoggerStub.debugMsg("Decoded String: " + userandpwdecoded);
		return authentic;
	}

	private void reply(ContentProducer cp) {
		EntityTemplate body = new EntityTemplate(cp);
		body.setContentType("text/html; charset=UTF-8");
		response.setEntity(body);
	}

	ContentProducer defaultPage = new ContentProducer() {
		@Override
		public void writeTo(final OutputStream outstream) throws IOException {
			OutputStreamWriter writer = new OutputStreamWriter(outstream,
					"UTF-8");
			writer.write("<html><body><h1>");
			writer.write("This is a http response");
			writer.write("</h1></body></html>");
			writer.flush();
		}
	};

	ContentProducer accessDenied = new ContentProducer() {
		// HTTP/1.1 401 Access Denied
		// WWW-Authenticate: Basic realm="My Server"
		// Content-Length: 0
		@Override
		public void writeTo(final OutputStream outstream) throws IOException {
			OutputStreamWriter writer = new OutputStreamWriter(outstream,
					"UTF-8");
			writer.write("HTTP/1.1 401 Access Denied\n");
			writer.write("WWW-Authenticate: Basic realm=\"ToureNPlaner\"\n");
			writer.write("Content-Length: 0\n");
			writer.flush();
		}
	};

	ContentProducer generalError = new ContentProducer() {
		@Override
		public void writeTo(final OutputStream outstream) throws IOException {
			OutputStreamWriter writer = new OutputStreamWriter(outstream,
					"UTF-8");
			writer.write("<html><body><h1>");
			writer.write("An error happened");
			writer.write("</h1></body></html>");
			writer.flush();
		}
	};

	private String algName;
	private final ComputeCore comCore;
	private HttpRequest request;
	// this object will send the HTTP response when the handle() method is
	// finished
	private HttpResponse response;
}
