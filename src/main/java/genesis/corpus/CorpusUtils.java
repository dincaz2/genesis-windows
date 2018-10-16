// Copyright (C) 2016 Fan Long, Peter Amidon, Martin Rianrd and MIT CSAIL 
// Genesis (A successor of Prophet for Java Programs)
// 
// This file is part of Genesis.
// 
// Genesis is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 2 of the License, or
// (at your option) any later version.
// 
// Genesis is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with Genesis.  If not, see <http://www.gnu.org/licenses/>.
package genesis.corpus;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import genesis.GenesisException;
import genesis.node.MyCtNode;

public class CorpusUtils {
	
	public static MyCtNode parseJavaAST(String path) {
		MyCtNode ret = null;
		try {
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(path));
			ret = MyCtNode.readFromStream(is);
			is.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new GenesisException("Exception happens when deserializing "
					+ path);
		}
		return ret;
	}
	
}
