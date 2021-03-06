/*
 * Copyright 2007-2017 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.corelib.web.socks;

import org.apache.commons.lang.StringUtils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Adds socks proxy to the system environment
 * Created by luthien on 09/08/2017.
 */
public class SocksProxy extends Authenticator{

    private ProxyAuthenticator auth;
    private String host;
    private String port;
    private String user;
    private String password;

    /**
     * Create a new SocksProxy setup. To add settings to the system environment you need to call the init method
     * @param host
     * @param port
     * @param user
     * @param password
     */
    public SocksProxy(String host, String port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    /**
     * Add the proxy configuration to the system environment. Note that this has to be done before any other connections
     * are setup
     */
    public void init() {
        System.setProperty("socksProxyHost", this.host);
        System.setProperty("socksProxyPort", this.port);
        if (StringUtils.isNotEmpty(this.user)) {
            System.setProperty("java.net.socks.username", this.user);
            System.setProperty("java.net.socks.password", this.password);
            auth = new ProxyAuthenticator(this.user, this.password);
            Authenticator.setDefault(auth);
        }
    }

    /**
     *
     * @return authentication details encoded in base64
     */
    public String getEncodedAuth(){
        return java.util.Base64.getEncoder().encodeToString((auth.user + ":" + auth.password).getBytes());
    }

    /**
     * @return authentication details
     */
    public ProxyAuthenticator getAuth(){
        return auth;
    }

    private static class ProxyAuthenticator extends Authenticator {

        private String user;
        private String password;

        public ProxyAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password.toCharArray());
        }
    }
}
