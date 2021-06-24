/**    
 *   Copyright (c) 2014 Zuora, Inc.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy of 
 *   this software and associated documentation files (the "Software"), to use copy, 
 *   modify, merge, publish the Software and to distribute, and sublicense copies of 
 *   the Software, provided no fee is charged for the Software.  In addition the
 *   rights specified above are conditioned upon the following:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   Zuora, Inc. or any other trademarks of Zuora, Inc.  may not be used to endorse
 *   or promote products derived from this Software without specific prior written
 *   permission from Zuora, Inc.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL
 *   ZUORA, INC. BE LIABLE FOR ANY DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES
 *   (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *   ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.zuora.rsa.security.decrypt;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.util.URIUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.zuora.rsa.security.PublicKeyBuilder;
import com.zuora.rsa.security.decrypt.FieldDecrypter;
/**
 * <p><b>Function:</b> It is a tool, which can be used to decrypt signature with public key.
 *
 * @author Tony Liu
 * @since Dec 15, 2014 10:33:00 AM
 * @see
 */
public class SignatureDecrypter {
    private static final String DELIM="#";
    
    public enum SignatureType {
        Advanced,
        Basic;
    }
    
    /**
     * Decrypt signature with public key, and get string returned.
     * 
     * @param signature: signature generated by Zuora.
     * @param publicKey: public key generated by Zuora.
     * @return
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     */
    public static final String decryptAsString(String signature, String publicKey) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        
        byte[] decoded = Base64.decodeBase64(signature.getBytes(Charset.forName("UTF-8")));
        Cipher encrypter = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encrypter.init(Cipher.DECRYPT_MODE, PublicKeyBuilder.build(publicKey));
        return new String(encrypter.doFinal(decoded));
    }
    
    /**
     * Using this method, you have to construct the encryptedString based on our guideline by yourself.
     * @param signature
     * @param encryptedString
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static final boolean verifyAdvancedSignature(String signature, String encryptedString, String publicKey) throws Exception {
        Signature instance = Signature.getInstance("SHA512withRSA");
        instance.initVerify((PublicKey) PublicKeyBuilder.build(publicKey));
        instance.update(encryptedString.getBytes() );
        System.out.println("The signature for SHA512WithRSA:" +  signature);
        System.out.println("The byte array of signature for SHA512WithRSA encryption " +  Base64.decodeBase64(signature).toString());
        return instance.verify( Base64.decodeBase64(signature));
    }
    
    /**
     * We construct the encryptedString for you through getting the value from http request directly.
     * @param request
     * @param callBackURL
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static final boolean verifyAdvancedSignature(HttpServletRequest request, String callBackURL, String publicKey) throws Exception {
        if (callBackURL == null || request == null || publicKey == null) {
             return false;
        }
          
        String lowerCase = callBackURL.toLowerCase();
          
        String queryPath = lowerCase.startsWith("http") ? URIUtil.getPathQuery(lowerCase) : lowerCase;
          
          
        StringBuilder encryptedString = new StringBuilder();
        encryptedString.append( "/hpm2samplecodejsp/callback.jsp");
        encryptedString.append( DELIM + request.getParameter("tenantId") );
        encryptedString.append( DELIM + request.getParameter("token"));
        encryptedString.append( DELIM + request.getParameter("timestamp"));
        encryptedString.append( DELIM + FieldDecrypter.decrypt(request.getParameter("pageId"), publicKey ));
        
        encryptedString.append( DELIM + (request.getParameter("errorCode") == null?"":request.getParameter("errorCode") ));
        
        encryptedString.append( DELIM + (request.getParameter("field_passthrough1") == null? "":request.getParameter("field_passthrough1")));
        encryptedString.append( DELIM + (request.getParameter("field_passthrough2") == null? "":request.getParameter("field_passthrough2")));
        encryptedString.append( DELIM + (request.getParameter("field_passthrough3") == null? "":request.getParameter("field_passthrough3")));
        encryptedString.append( DELIM + (request.getParameter("field_passthrough4") == null? "":request.getParameter("field_passthrough4")));
        encryptedString.append( DELIM + (request.getParameter("field_passthrough5") == null? "":request.getParameter("field_passthrough5")));
        
        encryptedString.append( DELIM + FieldDecrypter.decrypt(request.getParameter("refId"), publicKey) );

        boolean isSignatureValid = false;
        
        String signature = null;
        System.out.println("Charset:" + request.getCharacterEncoding() );
        System.out.println("Query String:" +  request.getQueryString() );

        String[] parameters = request.getQueryString().split("&");
        for(String parameter: parameters){
            String[] keyValue = parameter.split("=");
            if( keyValue.length>1 && "signature".equals(keyValue[0]) ){
                signature = keyValue[1];
                break;
            }
        }
        System.out.println("Advanced Signature String:" + signature );
        //isSignatureValid = SignatureDecrypter.verifyAdvancedSignature(new String(request.getParameter("signature").getBytes("iso-8859-1"), "UTF-8"), encryptedString.toString(), publicKeyString);
        return SignatureDecrypter.verifyAdvancedSignature(URLDecoder.decode( signature, "UTF-8"), encryptedString.toString(), publicKey);

    }
    /**
     * Decrypt signature with public key, and get string returned.
     * 
     * @param signature: signature generated by Zuora.
     * @param publicKey: public key generated by Zuora.
     * @return
     * @throws Exception 
     */
    public static final Map<Key, String> decryptAsMap(String signature, String publicKey) throws Exception {
        String decryptedSignature = decryptAsString(signature, publicKey);
        if(null != decryptedSignature) {
            Map<Key, String> maps = new HashMap<Key, String>();
            StringTokenizer st = new StringTokenizer(decryptedSignature,"#");
            
            maps.put(Key.Url, st.nextToken());
            maps.put(Key.TenantId, st.nextToken());
            maps.put(Key.Token, st.nextToken());
                maps.put(Key.Timestamp, st.nextToken());
            maps.put(Key.PageId, st.nextToken());
            
            return maps;
        }
        return null;
    }

    public static enum Key {
        Url, TenantId, Token, Timestamp, PageId
    }
}
