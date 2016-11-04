

import org.mule.transformer.AbstractMessageTransformer;
import org.mule.api.*;
import org.mule.util.Base64;

import org.apache.commons.io.IOUtils;

import javax.activation.DataHandler;

import java.io.*;
import java.util.*;

public class AttachmentToBase64Transformer extends AbstractMessageTransformer {
    
    public Object transformMessage(MuleMessage src, String outputEncoding) {
        try {
//System.out.println(">>> lkName is " + src.getInvocationProperty("lkName"));
//System.out.println(">>> attachment is " + src.getOutboundAttachment((String)src.getInvocationProperty("lkName")));
			
//            DataHandler dh = src.getOutboundAttachment((String)src.getInvocationProperty("lkName"));
//            InputStream stream = src.getPayload();
//            InputStream is = dh.getInputStream();
//            byte[] contentBytes = IOUtils.toByteArray(is);
        	byte[]  contentBytes = src.getInvocationProperty("file");
//        	 byte[]  contentBytes = (byte[]) src.getPayload();
			src.setInvocationProperty("base64Key", contentBytes);
				
			 com.sforce.soap.partner.SaveResult sr = src.getInvocationProperty("LicenseObject");
			 
System.out.println("************* LICENSE OBJECT ID IS " + sr.getId());          

            src.setInvocationProperty("LicenseKeyId", sr.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return src;
    }
}