/*
 * The MIT License
 *
 * Copyright 2017 Adam Currie.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.adam_currie.fusenotesshared;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Base64;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;

/*
 * Name     ECDSAUtil
 * Purpose  Utility class for ECDSA keys and signing.
 */
public class ECDSAUtil{
    
    /*
     * Method           main
     * Description      test main  
     */
    public static void main(String[] args) throws InvalidKeyException{
        AsymmetricCipherKeyPair pair = generateKeyPair();
        ECPrivateKeyParameters priv = (ECPrivateKeyParameters)pair.getPrivate();
        ECPublicKeyParameters pub = (ECPublicKeyParameters)pair.getPublic();
        System.out.println("private key: " + ECDSAUtil.toBase64(priv));
        System.out.println("Public key:  " + toBase64(pub));
        
        System.out.println("public key from private: " + toBase64(ECDSAUtil.toPublicKeyParams(priv)));
        
        System.out.println("public key to base64, back to key, and back to base64: " + 
                toBase64(toPublicKeyParams(toBase64(pub)))
        );
        
        System.out.println("private key to base64, back to key, and back to base64: " + 
                ECDSAUtil.toBase64(toPrivateKeyParams(ECDSAUtil.toBase64(priv)))
        );
        
        return;
    }
    
    public static ECPublicKeyParameters toPublicKeyParams(ECPrivateKeyParameters privateKeyParams){
        //get params
        X9ECParameters curveParams = SECNamedCurves.getByName("secp256r1");
        ECDomainParameters domainParams = new ECDomainParameters(
            curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH(), curveParams.getSeed());
        
        ECPoint q = domainParams.getG().multiply(privateKeyParams.getD());
        
        return new ECPublicKeyParameters(q, domainParams);
    }

    public static String toBase64(ECPrivateKeyParameters privateKeyParams){
        //todo: maybe compress this        
        return Base64.getEncoder().encodeToString(privateKeyParams.getD().toByteArray());
    }
    
    public static String toBase64(ECPublicKeyParameters publicKeyParams){
        return Base64.getEncoder().encodeToString(publicKeyParams.getQ().getEncoded(true));
    }

    public static ECPrivateKeyParameters toPrivateKeyParams(String privateKeyBase64) throws InvalidKeyException{
        try{
            //get params
            X9ECParameters curveParams = SECNamedCurves.getByName("secp256r1");
            ECDomainParameters domainParams = new ECDomainParameters(
                curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH(), curveParams.getSeed());

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);

            BigInteger keyInt = new BigInteger(keyBytes);
            return new ECPrivateKeyParameters(keyInt, domainParams);
        }catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex){
            throw new InvalidKeyException("Invalid key length.");
        }
    }

    public static ECPublicKeyParameters toPublicKeyParams(String publicKeyBase64) throws InvalidKeyException{
        try{
            //get params
            X9ECParameters curveParams = SECNamedCurves.getByName("secp256r1");
            ECDomainParameters domainParams = new ECDomainParameters(
                curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH(), curveParams.getSeed());

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            ECPoint point = domainParams.getCurve().decodePoint(keyBytes);

            return new ECPublicKeyParameters(point, domainParams);
        }catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex){
            throw new InvalidKeyException("Invalid key length.");
        }
    }

    public static AsymmetricCipherKeyPair generateKeyPair(){
        //get params
        X9ECParameters curveParams = SECNamedCurves.getByName("secp256r1");
        ECDomainParameters domainParams = new ECDomainParameters(
            curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH(), curveParams.getSeed());
        
        //generate key
        ECKeyGenerationParameters keyGenParams = new ECKeyGenerationParameters(domainParams, new SecureRandom());
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(keyGenParams);

        return generator.generateKeyPair();
    }

    public static boolean checkKeyValid(String keyStr){
        if(keyStr == null || keyStr.isEmpty()){
            return false;
        }
        try{            
            //it is defined behaviour for the key to be made valid if it isn't
            //so if it's changed to a valid key then the original is invalid
            ECPrivateKeyParameters potentialPrivateKey = toPrivateKeyParams(keyStr);            
            return toBase64(potentialPrivateKey).equals(keyStr);
        }catch(InvalidKeyException ex){
            return false;
        }
    }

    public static String generatePrivateKeyStr(){
        ECPrivateKeyParameters key = (ECPrivateKeyParameters)generateKeyPair().getPrivate();
        return toBase64(key);
    }

    public static byte[] toBytesFromBase64(String base64Str){
        return Base64.getDecoder().decode(base64Str);
    }

    public static byte[] toPublicKeyFromPrivate(byte[] privateKey) throws InvalidKeyException{
        try{
            //get params
            X9ECParameters curveParams = SECNamedCurves.getByName("secp256r1");
            ECDomainParameters domainParams = new ECDomainParameters(
                curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH(), curveParams.getSeed());

            BigInteger keyInt = new BigInteger(privateKey);
            ECPublicKeyParameters pub = ECDSAUtil.toPublicKeyParams(new ECPrivateKeyParameters(keyInt, domainParams));
            
            return pub.getQ().getEncoded(true);
        }catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex){
            throw new InvalidKeyException("Invalid key length.");
        }
    }

    public static ECPrivateKeyParameters toPrivateKeyParams(byte[] keyBytes) throws InvalidKeyException{
        try{
            //get params
            X9ECParameters curveParams = SECNamedCurves.getByName("secp256r1");
            ECDomainParameters domainParams = new ECDomainParameters(
                curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH(), curveParams.getSeed());

            BigInteger keyInt = new BigInteger(keyBytes);
            return new ECPrivateKeyParameters(keyInt, domainParams);
        }catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex){
            throw new InvalidKeyException("Invalid key length.");
        }
    }
    
}
