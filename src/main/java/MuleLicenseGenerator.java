

import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleEventContext;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.RequestContext;

import com.mulesoft.licm.*;
import com.mulesoft.licm.feature.*;

import java.util.*;
import java.io.*;
import java.text.*;

import javax.naming.ldap.Rdn;
import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleLicenseGenerator implements Callable {

	private static final Logger logger = LoggerFactory.getLogger(MuleLicenseGenerator.class);

	public Object onCall(MuleEventContext eventContext) throws Exception {

		Map requestMap = (Map) eventContext.getMessage().getPayload();

		return generateLicense(requestMap);
	}

	public MuleMessage generateLicense(String product, String day, String month, String year, String name, String email,
			String phone, String company, String country, String entitlements, String subscriptionId) throws Exception {

		Map requestMap = new HashMap();

		requestMap.put("product", product);
		requestMap.put("day", day);
		requestMap.put("month", month);
		requestMap.put("year", year);
		requestMap.put("name", name);
		requestMap.put("email", email);
		requestMap.put("phone", phone);
		requestMap.put("company", company);
		requestMap.put("country", country);
		requestMap.put("entitlements", entitlements);
		requestMap.put("subscriptionId", subscriptionId);

		return generateLicense(requestMap);
	}

	public MuleMessage generateLicense(Map requestMap) throws Exception {
		String product = (String) requestMap.get("product");

		EnterpriseLicenseKeyRequest request = LicenseManagementFactory.getInstance().createLicenseRequest(product);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setLenient(false);

		String month = requestMap.get("month").toString();
		if (month == null || "".equals(month.trim()))
			month = "01";
		if (month.length() < 2)
			month = "0" + month;

		String day = (String) requestMap.get("day").toString();
		if (day == null || "".equals(day.trim()))
			day = "01";
		if (day.length() < 2)
			day = "0" + day;

		String dateStr = (String) requestMap.get("year").toString() + "-" + month + "-" + day;
		request.setExpirationDate(dateFormat.parse(dateStr));

		String escapedName = Rdn.escapeValue(((String) requestMap.get("name")).trim());
		request.setContactName(escapedName);

		String escapedEmail = Rdn.escapeValue(((String) requestMap.get("email")).trim());
		request.setContactEmailAddress(Rdn.escapeValue((String) requestMap.get("email")));

		String escapedPhone = Rdn.escapeValue(((String) requestMap.get("phone")).trim());
		request.setContactTelephone(escapedPhone);

		String escapedCompany = Rdn.escapeValue(((String) requestMap.get("company")).trim());
		request.setContactCompany(escapedCompany);

		request.setContactCountry((String) requestMap.get("country"));

		File tempFile = File.createTempFile("lic", "lic");
		request.setLicenseKeyFile(tempFile);

		String entitlement = "";
		if (!"tcat".equals(product.trim())) {
			entitlement = requestMap.get("entitlements").toString();
			entitlement = entitlement.replace('[',' ').replace(']',' ').replaceAll(" +", "").trim();
			
		} else {
			entitlement = "tcat";
		}
		
		logger.info("****** ENTITLEMENTS ARE: " + entitlement);

		LKGEntitlementsConfigReader.read(entitlement);
		FeatureSet featureSet = (FeatureSet) LKGEntitlementsConfigReader.getFeatureSet();
		List featuresList = featureSet.getFeatures();
		Iterator iterator = featuresList.iterator();
		while (iterator.hasNext()) {
			Feature feature = (Feature) iterator.next();
			request.addFeature(feature);
		}

		LicenseManager licManager = LicenseManagementFactory.getInstance().createLicenseManager(product);
		EnterpriseLicense key = licManager.create(request);

		InputStream is = new FileInputStream(tempFile);
		long length = tempFile.length();
		byte[] bytes = new byte[(int) length];
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}
		is.close();

		// System.out.println("&&&& KEY SUCCESSFULLY GENERATED: " + key);

		MuleMessage msg = RequestContext.getEventContext().getMessage();
		msg.setInvocationProperty("downloadUrl",
				"<br><br><a href=\"/license/" + tempFile.getAbsolutePath() + "\">DOWNLOAD LICENSE FILE</a>");
		String subscriptionId = (String) requestMap.get("subscriptionId");
		if (subscriptionId == null)
			subscriptionId = "n/a";
		msg.setInvocationProperty("subscriptionId", subscriptionId);

		msg.setInvocationProperty("expDate", dateStr);
		msg.setInvocationProperty("lkName", product.trim() + "-license.lic");
		msg.setOutboundProperty("subject", "[LICENSE KEY] New license key generated - "
				+ (String) requestMap.get("company") + "," + (String) requestMap.get("country"));
		ByteArrayDataSource attachmentDS = new ByteArrayDataSource(bytes, "application/octet-stream");
		attachmentDS.setName(product.trim() + "-license.lic");
		msg.addOutboundAttachment(product.trim() + "-license.lic", new DataHandler(attachmentDS));
		msg.setPayload("LICENSE KEY SUCCESSFULLY GENERATED! " + key.toString() + "<br><br><a href=\"/license/"
				+ tempFile.getAbsolutePath() + "\">DOWNLOAD LICENSE FILE</a>");
		return msg;

	}

}