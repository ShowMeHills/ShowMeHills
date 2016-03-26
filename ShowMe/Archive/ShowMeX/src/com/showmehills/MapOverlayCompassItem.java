/*
    Copyright 2012 Nik Cain nik@showmehills.com
    
    This file is part of ShowMeHills.

    ShowMeHills is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ShowMeHills is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ShowMeHills.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.showmehills;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MapOverlayCompassItem extends ItemizedOverlay<OverlayItem> {

	public float mBearing = 0;
	
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	@Override
	protected boolean onTap(int index) {
		return true;
	}
	@Override
    public void draw(android.graphics.Canvas canvas, MapView mapView, boolean shadow)
    {
        canvas.save();
                 
        Matrix m = canvas.getMatrix();
        for (OverlayItem item : mOverlays) {
        	Point pnt = mapView.getProjection().toPixels(item.getPoint(), null );
        	m.preRotate(mBearing, pnt.x, pnt.y);     
        }
        canvas.setMatrix(m);         
        super.draw(canvas, mapView, false);
         
        canvas.restore();

    }

	public MapOverlayCompassItem(Drawable defaultMarker, Context context) {		
		super(boundCenter(defaultMarker));
	}
	
	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

}
