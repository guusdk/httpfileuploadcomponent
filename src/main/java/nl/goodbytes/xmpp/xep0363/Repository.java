/*
 * Copyright (c) 2017-2023 Guus der Kinderen. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.goodbytes.xmpp.xep0363;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stores uploaded files for later retrieval.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public interface Repository
{
    void initialize() throws IOException;

    void destroy();

    boolean contains( SecureUniqueId uuid );

    String calculateETagHash( SecureUniqueId uuid );

    String getContentType( SecureUniqueId uuid );

    long getSize( SecureUniqueId uuid );

    // For reading data.
    InputStream getInputStream( SecureUniqueId uuid ) throws IOException;

    // For writing data.
    OutputStream getOutputStream( SecureUniqueId uuid ) throws IOException;

    boolean delete(SecureUniqueId uuid) throws IOException;
}
