package kr.ac.knu.odego.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import kr.ac.knu.odego.R;
import kr.ac.knu.odego.item.BusStopItem;
import kr.ac.knu.odego.item.FavoriteItem;
import kr.ac.knu.odego.item.RouteItem;

/**
 * Created by BHI on 2016-05-15.
 */
public class ListItemView extends LinearLayout {
    private TextView mItemName, mItemDetail;
    private ImageView mItemIcon;
    final int BUSSTOP_ICON = R.drawable.bus_stop_01;
    final int BUS_ICON = R.drawable.bus_01;

    public ListItemView(Context context, FavoriteItem item) {
        super(context);
        init(context);

        setItemNameText(item.getName());
        setItemDetailText(item.getUrl());
        if(item.getType() == FavoriteItem.BUS_STOP)
            setItemIcon(BUSSTOP_ICON);
        else
            setItemIcon(BUS_ICON);
    }

    public ListItemView(Context context, RouteItem item) {
        super(context);
        init(context);

        setItemNameText(item.getRoNo());
        setItemIcon(BUS_ICON);
    }

    public ListItemView(Context context, BusStopItem item) {
        super(context);
        init(context);

        setItemNameText(item.getBsNm());
        setItemIcon(BUSSTOP_ICON);
    }


    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.list_item, this, true);

        mItemName = (TextView) findViewById(R.id.item_name);
        mItemDetail = (TextView) findViewById(R.id.item_detail);
        mItemIcon = (ImageView) findViewById(R.id.item_icon);
    }

    public void setItemNameText(String str) {
        mItemName.setText(str);
    }

    public void setItemDetailText(String str) {
        mItemDetail.setText(str);
    }

    public void setItemIcon(int icon) {
        mItemIcon.setImageResource(icon);
    }
}