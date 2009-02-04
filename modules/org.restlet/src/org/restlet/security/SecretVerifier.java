/**
 * Copyright 2005-2009 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of the following open
 * source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.sun.com/cddl/cddl.html
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.security;

import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Verifier of identifier/secret couples.
 * 
 * @author Jerome Louvel
 */
public abstract class SecretVerifier extends Verifier {

    /**
     * Creates a user principal for the given user identifier.
     * 
     * @param identifier
     *            The user identifier.
     * @return A user principal.
     */
    protected UserPrincipal createUserPrincipal(String identifier, char[] secret) {
        return new UserPrincipal(new User(identifier, secret));
    }

    /**
     * Verifies that the proposed secret is correct for the specified
     * identifier. By default, it compares the inputSecret with the one obtain
     * by the {@link #getSecret(String)} method and adds a new
     * {@link RolePrincipal} instance to the subject if successful.
     * 
     * @param identifier
     *            The user identifier.
     * @param inputSecret
     *            The proposed secret.
     * @return True if the proposed secret was correct and the subject updated.
     */
    @Override
    public int verify(Request request, Response response) {
        int result = RESULT_VALID;

        if (request.getChallengeResponse() == null) {
            result = RESULT_MISSING;
        } else if (verify(request.getChallengeResponse().getIdentifier(),
                request.getChallengeResponse().getSecret())) {
            // Add a principal for this identifier
            request.getClientInfo().getSubject().getPrincipals().add(
                    createUserPrincipal(request.getChallengeResponse()
                            .getIdentifier(), request.getChallengeResponse()
                            .getSecret()));
        } else {
            result = RESULT_INVALID;
        }

        return result;
    }

    /**
     * Verifies that the identifier/secret couple is valid.
     * 
     * @param identifier
     * @param inputSecret
     * @return
     */
    public abstract boolean verify(String identifier, char[] inputSecret);

}
