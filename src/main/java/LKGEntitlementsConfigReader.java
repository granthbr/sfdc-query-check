/*
 * $Id$
 * --------------------------------------------------------------------------------------
 *
 * (c) 2003-2008 MuleSource, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSource's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSource. If such an agreement is not in place, you may not use the software.
 */



import com.mulesoft.licm.feature.DefaultFeatureSet;
import com.mulesoft.licm.feature.Feature;
import com.mulesoft.licm.feature.FeatureSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LKGEntitlementsConfigReader
{

    private static final Logger logger = LoggerFactory.getLogger(LKGEntitlementsConfigReader.class);

    private static final String ENTITLEMENTS_PROPERTIES_FILE = "entitlements.xml";

    private static final String ENTITLEMENTS_ROOT = "entitlement";

    private static FeatureSet featureSet = new DefaultFeatureSet();

    /**
     * Reads enterprise.xml file and populates {@link #features} list
     */
    public static void read(String entitlementKey)
    {

        System.setProperty("java.util.prefs.PreferencesFactory",
            "com.mulesource.licm.pref.MulePreferencesFactory");

        try
        {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                ENTITLEMENTS_PROPERTIES_FILE);
            try
            {
                Preferences.importPreferences(is);
                is.close();
            }
            catch (IOException e)
            {
                logger.error(ENTITLEMENTS_PROPERTIES_FILE + " not found. Entitlements disabled.");
                return;
            }

            String[] entKeys = entitlementKey.split(",");
            
            if (featureSet.hasFeatures() == true) featureSet.clearFeatures();
             
            for (int x = 0; x < entKeys.length; x++) {
                Preferences entitlement = Preferences.userRoot().node(ENTITLEMENTS_ROOT).node(entKeys[x]);
                int i = 0;
                while (true)
                {
                    String value = entitlement.get(ENTITLEMENTS_ROOT + i, "none");
                    if (value.compareToIgnoreCase("none") == 0) break;

                    featureSet.addFeature(new Feature(value, value));
                    i++;
                }
            }
/*
            Preferences entitlement = Preferences.userRoot().node(ENTITLEMENTS_ROOT).node(entitlementKey);

            int i = 0;
            while (true)
            {
                // clear the features as we are reading them in fresh
                if (i == 0 && featureSet.hasFeatures() == true) featureSet.clearFeatures();
                String value = entitlement.get(ENTITLEMENTS_ROOT + i, "none");
             System.out.println("*************MLKG************** ENTITLEMENT VALUE" + value);
                if (value.compareToIgnoreCase("none") == 0) break;

                featureSet.addFeature(new Feature(value, value));
                i++;
            }
*/
        }
        catch (Exception e)
        {
            logger.error("Invalid " + ENTITLEMENTS_PROPERTIES_FILE, e);
        }
    }

    /**
     * @return FeatureSet for entitlement
     */
    public static FeatureSet getFeatureSet()
    {
        return featureSet;
    }

}