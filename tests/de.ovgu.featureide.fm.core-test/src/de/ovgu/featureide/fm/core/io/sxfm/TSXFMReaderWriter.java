/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2012  FeatureIDE team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.core.io.sxfm;

import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.io.IFeatureModelReader;
import de.ovgu.featureide.fm.core.io.IFeatureModelWriter;
import de.ovgu.featureide.fm.core.io.TAbstractFeatureModelReaderWriter;
import de.ovgu.featureide.fm.core.io.UnsupportedModelException;
import de.ovgu.featureide.fm.core.io.sxfm.SXFMReader;
import de.ovgu.featureide.fm.core.io.sxfm.SXFMWriter;

/**
 * Test class for SXFM reader and writer
 * 
 * @author Fabian Benduhn
 */
public class TSXFMReaderWriter extends TAbstractFeatureModelReaderWriter{

	/**
	 * @param file
	 * @throws UnsupportedModelException 
	 */
	public TSXFMReaderWriter(FeatureModel fm, String s) throws UnsupportedModelException {
		super(fm,s);
	}

	/* (non-Javadoc)
	 * @see de.ovgu.featureide.fm.core.io.TAbstractFeatureModelReaderWriter#getWriter(de.ovgu.featureide.fm.core.FeatureModel)
	 */
	@Override
	protected IFeatureModelWriter getWriter(FeatureModel fm) {
		return new SXFMWriter(fm);
	}

	/* (non-Javadoc)
	 * @see de.ovgu.featureide.fm.core.io.TAbstractFeatureModelReaderWriter#getReader(de.ovgu.featureide.fm.core.FeatureModel)
	 */
	@Override
	protected IFeatureModelReader getReader(FeatureModel fm) {
		return new SXFMReader(fm);
	}

	
	@Override
	public void testFeatureHidden(){
		
	}

}
