/**
 * Copyright (c) 2012-2022 Reficio (TM), Jesse Gallagher All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//
// $Id$
//

import aQute.bnd.osgi.Jar
import org.reficio.p2.utils.TestUtils as Util

File target = new File(basedir, 'target/repository/plugins')
Jar jarWithEE = new Jar(new File(target, "org.mockito.mockito-core_1.8.5.jar"))
Jar jarWithoutEE = new Jar(new File(target, "org.mockito.mockito-core_1.9.0.jar"))

assert Util.attr(jarWithEE, "Require-Capability").contains("osgi.ee")
assert Util.attr(jarWithoutEE, "Require-Capability") == null
