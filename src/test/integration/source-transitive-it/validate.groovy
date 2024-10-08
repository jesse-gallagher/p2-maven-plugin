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
File target = new File(basedir, 'target/repository/plugins')
assert target.exists()
assert target.listFiles().size() == 6

def files = target.listFiles().collect { it.name }

assert files.contains("org.mockito.mockito-core_1.9.0.jar")
assert files.contains("org.mockito.mockito-core.source_1.9.0.jar")

assert files.contains("org.hamcrest.core_1.1.0.jar")
assert files.contains("org.hamcrest.core.source_1.1.0.jar")

assert files.contains("org.objenesis_1.0.0.jar")
assert files.contains("org.objenesis.source_1.0.0.jar")