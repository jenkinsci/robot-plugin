/*
* Copyright 2008-2014 Nokia Solutions and Networks Oy
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package hudson.plugins.robot.model;

import java.io.Serializable;

/**
 * Class representing a single testsuite/category.
 *
 */
public class RobotResultStatistics implements Serializable {

	private static final long serialVersionUID = 1L;

	private long pass;
	private long fail;

	//backwards compatibility with old builds
	private transient String name;

	public long getPass() {
		return pass;
	}

	public void setPass(long pass) {
		this.pass = pass;
	}

	public long getFail() {
		return fail;
	}

	public void setFail(long fail) {
		this.fail = fail;
	}

	public long getTotal(){
		return fail + pass;
	}
}
