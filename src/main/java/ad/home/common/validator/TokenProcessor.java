package ad.home.common.validator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TokenProcessor {
    /**
     * The timestamp used most recently to generate a token value.
     */
    private long previous;

    /**
     * Protected constructor for TokenProcessor.  Use TokenProcessor.getInstance()
     * to obtain a reference to the processor.
     */
    private TokenProcessor() {
    }

    /**
     * Retrieves the singleton instance of this class.
     */
    public static TokenProcessor getInstance() {
        return TokenProcessorHolder.instance;
    }

    private static class TokenProcessorHolder{
        /**
         * The singleton instance of this class.
         */
        private static TokenProcessor instance = new TokenProcessor();
    }

    /**
     * <p>Return <code>true</code> if there is a transaction token stored in
     * the user's current session, and the value submitted as a request
     * parameter with this action matches it.  Returns <code>false</code>
     * under any of the following circumstances:</p>
     *
     * <ul>
     *
     * <li>No session associated with this request</li>
     *
     * <li>No transaction token saved in the session</li>
     *
     * <li>No transaction token included as a request parameter</li>
     *
     * <li>The included transaction token value does not match the transaction
     * token in the user's session</li>
     *
     * </ul>
     *
     * @param request The servlet request we are processing
     */
    public synchronized boolean isTokenValid(HttpServletRequest request, String tokenName) {
        return this.isTokenValid(request, false,tokenName);
    }

    /**
     * Return <code>true</code> if there is a transaction token stored in the
     * user's current session, and the value submitted as a request parameter
     * with this action matches it.  Returns <code>false</code>
     *
     * <ul>
     *
     * <li>No session associated with this request</li> <li>No transaction
     * token saved in the session</li>
     *
     * <li>No transaction token included as a request parameter</li>
     *
     * <li>The included transaction token value does not match the transaction
     * token in the user's session</li>
     *
     * </ul>
     *
     * @param request The servlet request we are processing
     * @param reset   Should we reset the token after checking it?
     */
    public synchronized boolean isTokenValid(HttpServletRequest request,
                                             boolean reset,String tokenName) {
        // Retrieve the current session for this request
        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        // Retrieve the transaction token from this session, and
        // reset it if requested
        String saved =
                (String) session.getAttribute(tokenName);

        if (saved == null) {
            return false;
        }

        if (reset) {
            this.resetToken(request,tokenName);
        }

        // Retrieve the transaction token included in this request
        String token = request.getParameter(tokenName);

        if (token == null) {
            return false;
        }

        return saved.equals(token);
    }

    /**
     * Reset the saved transaction token in the user's session.  This
     * indicates that transactional token checking will not be needed on the
     * next request that is submitted.
     *
     * @param request The servlet request we are processing
     */
    public synchronized void resetToken(HttpServletRequest request,String tokenName) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        session.removeAttribute(tokenName);
    }

    /**
     * Save a new transaction token in the user's current session, creating a
     * new session if necessary.
     *
     * @param request The servlet request we are processing
     */
    public synchronized void saveToken(HttpServletRequest request,String tokenName) {
//        HttpSession session = request.getSession();
        String tokenValue = generateToken(request);

//        if (token != null) {
//            session.setAttribute(tokenName, token);
//        }
        saveTokenByValue(request, tokenName, tokenValue);
    }

    public synchronized void saveTokenByValue(HttpServletRequest request,String tokenName,String tokenValue){
        HttpSession session = request.getSession();
        if (tokenValue != null) {
            session.setAttribute(tokenName, tokenValue);
        }
    }

    /**
     * Generate a new transaction token, to be used for enforcing a single
     * request for a particular transaction.
     *
     * @param request The request we are processing
     */
    public synchronized String generateToken(HttpServletRequest request) {
        HttpSession session = request.getSession();

        return generateToken(session.getId());
    }

    /**
     * Generate a new transaction token, to be used for enforcing a single
     * request for a particular transaction.
     *
     * @param id a unique Identifier for the session or other context in which
     *           this token is to be used.
     */
    public synchronized String generateToken(String id) {
        try {
            long current = System.currentTimeMillis();

            if (current == previous) {
                current++;
            }

            previous = current;

            byte[] now = new Long(current).toString().getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");

            md.update(id.getBytes());
            md.update(now);

            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Convert a byte array to a String of hexadecimal digits and return it.
     *
     * @param buffer The byte array to be converted
     */
    private String toHex(byte[] buffer) {
        StringBuffer sb = new StringBuffer(buffer.length * 2);

        for (int i = 0; i < buffer.length; i++) {
            sb.append(Character.forDigit((buffer[i] & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(buffer[i] & 0x0f, 16));
        }

        return sb.toString();
    }

}
