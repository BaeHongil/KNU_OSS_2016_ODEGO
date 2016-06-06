package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import io.realm.Realm;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.common.BusType;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;

/**
 * Created by Brick on 2016-05-30.
 */
public class BusPosInfoListAdapter extends BaseAdapter {
    private Context mContext;
    private Realm mRealm;
    private String busType;
    private BusPosInfo[] busPosInfos;
    private LayoutInflater inflater;
    private int busOffImg, busOnImg, busOnNsImg;

    public BusPosInfoListAdapter(Context mContext, Realm mRealm, String busType) {
        this.mContext = mContext;
        this.mRealm = mRealm;
        this.busType = busType;

        inflater = LayoutInflater.from(mContext);
        if( busType.equals( BusType.MAIN.getName() ) ) {
            busOffImg = R.drawable.busposinfo_main_bus_off;
            busOnImg = R.drawable.busposinfo_main_bus_on;
            busOnNsImg = R.drawable.busposinfo_main_bus_on_nonstep;
        } else if( busType.equals( BusType.BRANCH.getName() ) ) {
            busOffImg = R.drawable.busposinfo_branch_bus_off;
            busOnImg = R.drawable.busposinfo_branch_bus_on;
            busOnNsImg = R.drawable.busposinfo_branch_bus_on_nonstep;
        } else if( busType.equals( BusType.CIRCULAR.getName() ) ) {
            busOffImg = R.drawable.busposinfo_circular_bus_off;
            busOnImg = R.drawable.busposinfo_circular_bus_on;
            busOnNsImg = R.drawable.busposinfo_circular_bus_on_nonstep;
        } else {
            busOffImg = R.drawable.busposinfo_express_bus_off;
            busOnImg = R.drawable.busposinfo_express_bus_on;
            busOnNsImg = R.drawable.busposinfo_express_bus_on_nonstep;
        }
    }

    public void setBusPosInfos(BusPosInfo[] busPosInfos) {
        this.busPosInfos = busPosInfos;
    }

    @Override
    public int getCount() {
        if(busPosInfos == null)
            return 0;
        return busPosInfos.length;
    }

    @Override
    public Object getItem(int position) {
        if(busPosInfos == null)
            return null;
        return busPosInfos[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = null;
        BusPosInfoViewHolder viewHolder = null;
        BusPosInfo busPosInfo = busPosInfos[position];

        BusStop mBusStop = busPosInfo.getMBusStop();
        if(convertView==null) {
            itemView = inflater.inflate(R.layout.activity_busposinfo_list_item, parent, false);
            viewHolder = new BusPosInfoViewHolder(itemView);
            viewHolder.busStopId = mBusStop.getId();
            itemView.setTag( viewHolder );
        } else {
            itemView = convertView;
            viewHolder = (BusPosInfoViewHolder) itemView.getTag();
            viewHolder.busStopId = mBusStop.getId();
        }

        if( mRealm.where(Favorite.class).equalTo("mBusStop.id", mBusStop.getId()).count() > 0 )
            viewHolder.favoriteBtn.setChecked(true);
        else
            viewHolder.favoriteBtn.setChecked(false);
        viewHolder.busStopName.setText(mBusStop.getName());
        viewHolder.busStopNo.setText(mBusStop.getNo());

        // 버스가 없을 때
        if( busPosInfo.getBusId() == null ) {
            viewHolder.busIcon.setImageResource(busOffImg);
            viewHolder.busId.setVisibility(View.INVISIBLE);
            return itemView;
        }

        // 버스가 있을 때
        if( busPosInfo.isNonStepBus() )
            viewHolder.busIcon.setImageResource(busOnNsImg);
        else
            viewHolder.busIcon.setImageResource(busOnImg);
        String busId = busPosInfo.getBusId();
        String busIdNum = busId.substring(busId.length() - 4, busId.length());
        viewHolder.busId.setText(busIdNum);
        viewHolder.busId.setVisibility(View.VISIBLE);

        return itemView;
    }

    private class BusPosInfoViewHolder implements View.OnClickListener {
        public String busStopId;

        public View itemView;
        public ToggleButton favoriteBtn;
        public TextView busStopName;
        public TextView busStopNo;
        public ImageView busIcon;
        public TextView busId;

        public BusPosInfoViewHolder(View itemView) {
            this.itemView = itemView;
            favoriteBtn = (ToggleButton) itemView.findViewById(R.id.favorite_btn);
            favoriteBtn.setOnClickListener(this);
            busStopName = (TextView) itemView.findViewById(R.id.busstop_name);
            busStopNo = (TextView) itemView.findViewById(R.id.busstop_no);
            busIcon = (ImageView) itemView.findViewById(R.id.bus_icon);
            busId = (TextView) itemView.findViewById(R.id.bus_id);
        }

        @Override
        public void onClick(View v) {
            if( v == favoriteBtn ) {
                if( !favoriteBtn.isChecked() ) { // 즐겨찾기 해제
                    RealmTransaction.deleteBusStopFavorite(mRealm, busStopId);
                    return;
                }
                // 즐겨찾기 설정
                RealmTransaction.createBusStopFavorite(mRealm, busStopId);
            }
        }
    }

}