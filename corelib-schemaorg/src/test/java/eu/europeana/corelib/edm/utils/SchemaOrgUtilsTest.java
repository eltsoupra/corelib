package eu.europeana.corelib.edm.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/corelib-web-context.xml", "/corelib-web-test.xml"})
public class SchemaOrgUtilsTest {

	static String FULL_BEAN_FILE = "/schemaorg/fullbean.json";
	
    /**
     * Test schema.org generation and serialization
     */
    @Test
    public void toSchemaOrgTest() throws IOException, URISyntaxException {
        FullBeanImpl bean = MockFullBean.mock();
        String output = SchemaOrgUtils.toSchemaOrg(bean);
        Assert.assertNotNull(output);

        //Used for testing purposes, use the following call to update the expected output whenever the serialization is updated
//        writeToFile(output);

        try (InputStream stream = getClass().getResourceAsStream(FULL_BEAN_FILE)) {
            String expectedOutput = IOUtils.toString(stream, StandardCharsets.UTF_8);
            //we cannot string compare until the ordering of properties is implemented
            //still, a fast indication that the output was changed will be indicated through the length of the string
            assertEquals(expectedOutput.length(), output.length());
        }
    }

	void writeToFile(String output) throws IOException, URISyntaxException {
		URI outputFilePath = getClass().getResource(FULL_BEAN_FILE).toURI();
		File outputFile = new File(outputFilePath);
		FileUtils.write(outputFile, output, StandardCharsets.UTF_8);
	}

}