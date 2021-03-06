package kr.ac.knu.odego.activity;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmResults;
import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.adapter.BusPosInfoListAdapter;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;
import kr.ac.knu.odego.item.Route;

public class BusPosInfoActivity extends ObsvBaseActivity {

    private View mHeaderView;
    private View HeaderContentsView;
    private Toolbar mToolbarView;
    private View mListBackgroundView;
    private ObservableListView mListView;
    private BusPosInfoListAdapter mListAdapter;
    private int headerHeight;

    private Parser mParser = Parser.getInstance();
    private Realm mRealm;
    private RealmResults<Favorite> favoriteRealmResults;
    private Route mRoute;
    private String routeId = "3000306000";
    private BusStop[][] busStops = new BusStop[2][]; // index 0은 역방향, index 1은 정방향

    private int themeColor;
    private boolean isForward = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busposinfo);

        mRealm = Realm.getDefaultInstance();
        routeId = getIntent().getExtras().getString("routeId");
        mRoute = mRealm.where(Route.class).equalTo("id", routeId).findFirst();

        // 리스트 뷰 설정
        mListView = (ObservableListView) findViewById(R.id.list);
        mListView.setScrollViewCallbacks(this);
        mListAdapter = new BusPosInfoListAdapter(this, mRealm, mRoute.getType());
        mListView.setAdapter(mListAdapter);
        headerHeight = getResources().getDimensionPixelSize(R.dimen.busposinfo_header_height);
        setPaddingView(mListView, headerHeight);
        setUpProgressLayout(R.string.refreshing_busposinfo_data, headerHeight);  // 새로고침 레이아웃 설정

        // 도착정보 얻기
        GetBusPosInfoAsyncTask getBusPosinfoAsyncTask = new GetBusPosInfoAsyncTask();
        getBusPosinfoAsyncTask.execute(isForward);
        favoriteRealmResults = mRealm.where(Favorite.class).equalTo("mRoute.id", routeId).findAll(); // 버스정류장 즐겨찾기 여부

        mHeaderView = findViewById(R.id.header);
        mHeaderView.bringToFront();
        HeaderContentsView = mHeaderView.findViewById(R.id.header_contents);
        // 현재 노선상세정보 있는지 확인후 노선정보 등록
        if( mRoute.getUpdatedDetail() != null && OdegoApplication.isToday( mRoute.getUpdatedDetail() ) )
            setHeaderData();
        else {
            GetRouteDetailInfoAsyncTask getRouteDetailInfoAsyncTask = new GetRouteDetailInfoAsyncTask();
            getRouteDetailInfoAsyncTask.execute();
        }

        // 테마색상 노선유형에 따라 설정
        String routeType = mRoute.getType();
        if (RouteType.MAIN.getName().equals( routeType ))
            themeColor = ContextCompat.getColor(this, R.color.main_bus);
        else if (RouteType.EXPRESS.getName().equals( routeType ))
            themeColor = ContextCompat.getColor(this, R.color.express_bus);
        else if (RouteType.CIRCULAR.getName().equals( routeType ))
            themeColor = ContextCompat.getColor(this, R.color.circular_bus);
        else
            themeColor = ContextCompat.getColor(this, R.color.branch_bus);

        if (Build.VERSION.SDK_INT >= 21)  // 상태바 색상 변경
            getWindow().setStatusBarColor(themeColor);
        mHeaderView.setBackgroundColor(themeColor);

        mToolbarView = (Toolbar)findViewById(R.id.toolbar);
        mToolbarView.setTitle(mRoute.getNo());
        mToolbarView.bringToFront();

        // 툴바 색상 설정
        mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, themeColor));
        setSupportActionBar(mToolbarView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기 아이콘 표시

        // 플로팅액션버튼 설정
        FloatingActionButton refreshBtn = (FloatingActionButton) findViewById(R.id.refresh);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetBusPosInfoAsyncTask getBusPosinfoAsyncTask = new GetBusPosInfoAsyncTask();
                getBusPosinfoAsyncTask.execute(isForward);

                Snackbar.make(view, getString(R.string.refresh_busposinfo_data), Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });

        // mListBackgroundView makes ListView's background except header view.
        mListBackgroundView = findViewById(R.id.list_background);
    }

    private void setHeaderData() {
        TextView headerRouteType = (TextView) HeaderContentsView.findViewById(R.id.route_type);
        TextView headerRouteNo = (TextView) HeaderContentsView.findViewById(R.id.route_no);
        TextView headerTotalRoute = (TextView) HeaderContentsView.findViewById(R.id.total_route);
        headerTotalRoute.setSelected(true);
        TextView headerTotalRouteTime = (TextView) HeaderContentsView.findViewById(R.id.total_route_time);
        TextView headerInterval = (TextView) HeaderContentsView.findViewById(R.id.interval);
        headerRouteType.setText(mRoute.getType());
        headerRouteNo.setText(mRoute.getNo());
        headerTotalRoute.setText(mRoute.getStartBusStopName() + " <--> " + mRoute.getEndBusStopName());
        headerTotalRouteTime.setText(
                String.format("%02d:%02d ~ %02d:%02d",
                        mRoute.getStartHour(), mRoute.getStartMin(), mRoute.getEndHour(), mRoute.getEndMin()
                ));
        headerInterval.setText(
                String.format("평일 %02d분 / 주말 %02d분",
                        mRoute.getInterval(), mRoute.getIntervalSun())
        );

        Switch swc = (Switch)findViewById(R.id.switch1);
        swc.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton cb, boolean isChecking) {
                isForward = !isChecking;
                GetBusPosInfoAsyncTask getBusPosinfoAsyncTask = new GetBusPosInfoAsyncTask();
                getBusPosinfoAsyncTask.execute(isForward);
            }
        });

        swc.setFocusable(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( mRealm != null)
            mRealm.close();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.favorite_menu, menu);
        MenuItem favoriteMenuItem = menu.findItem(R.id.action_favorite);
        if( favoriteRealmResults.size() > 0 ) {
            favoriteMenuItem.setIcon(R.drawable.favorite_on);
            favoriteMenuItem.setChecked(true);
        }
        else {
            favoriteMenuItem.setIcon(R.drawable.favorite_off);
            favoriteMenuItem.setChecked(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_favorite:
                boolean isChecked = !item.isChecked();
                item.setChecked(isChecked);
                if( isChecked ) {
                    item.setIcon(R.drawable.favorite_on);
                    RealmTransaction.createRouteFavorite(mRealm, routeId);
                } else {
                    item.setIcon(R.drawable.favorite_off);
                    RealmTransaction.deleteRouteFavorite(mRealm, routeId);
                }

                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        onScrollChanged(mListView.getCurrentScrollY(), false, false);
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        float alpha = Math.min(1, (float) scrollY / headerHeight ); // 헤더에 적용할 알파
        if( alpha == 1 ) { // 헤더가 사라졌을 때 툴바 텍스트 나타나도록 알파 설정
            mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(1, themeColor));
            float textAlpha = Math.min(1, (float) (scrollY-headerHeight) / (getActionBarSize()/2) );
            mToolbarView.setTitleTextColor(ScrollUtils.getColorWithAlpha(textAlpha, Color.WHITE));
        }
        else { // 헤더가 아직 있을 때 툴바 투명
            mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, themeColor));
            mToolbarView.setTitleTextColor(ScrollUtils.getColorWithAlpha(0, Color.WHITE));
        }

        HeaderContentsView.setAlpha(1-alpha); // 헤더전체뷰 알파 설정

        // Translate list background
        mHeaderView.setTranslationY(-scrollY / 2);
        mListBackgroundView.setTranslationY(Math.max(0, -scrollY + headerHeight));
    }

    // 노선 상세정보 가져오는 쓰레드
    private class GetRouteDetailInfoAsyncTask extends AsyncTask<Void, String, Route> {

        @Override
        protected Route doInBackground(Void... params) {
            Realm mRealm = null;
            try {
                mRealm = Realm.getDefaultInstance();
                mParser.getRouteDetailInfo(mRealm, routeId);
            } catch (IOException e) {
                publishProgress( getString(R.string.network_error_msg) );
            } finally {
                if(mRealm != null)
                    mRealm.close();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getBaseContext(), values[0], Toast.LENGTH_LONG);
        }

        @Override
        protected void onPostExecute(Route route) {
            setHeaderData();
        }
    }

    // 버스도착정보 가져오는 쓰레드
    private class GetBusPosInfoAsyncTask extends AsyncTask<Boolean, String, BusPosInfo[]> {

        @Override
        protected void onPreExecute() {
            mListView.setVisibility(View.GONE);
            getProgressLayout().setVisibility(View.VISIBLE);
        }

        @Override
        protected BusPosInfo[] doInBackground(Boolean... params) {
            boolean isForward = params[0];

            try {
                BusPosInfo[] busPosInfos = mParser.getBusPosInfos(routeId, isForward);
                return busPosInfos;
            } catch (IOException e) {
                publishProgress( getString(R.string.network_error_msg) );
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getBaseContext(), values[0], Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(BusPosInfo[] busPosInfos) {
            if( busPosInfos == null )
                return;

            int index = isForward ? 1 : 0;
            if( busStops[index] == null ) { // 버스정류장의 노선들 정보가 없을 때
                busStops[index] = new BusStop[busPosInfos.length];
                for (int i = 0; i < busPosInfos.length; i++) {
                    BusPosInfo busPosInfo = busPosInfos[i];
                    String busStopId = busPosInfo.getBusStopId();
                    BusStop mBusStop = mRealm.where(BusStop.class).equalTo("id", busStopId).findFirst();

                    busStops[index][i] = mBusStop;
                    busPosInfo.setMBusStop(mBusStop);
                }
            } else {
                for (int i = 0; i < busPosInfos.length; i++)
                    busPosInfos[i].setMBusStop(busStops[index][i]);
            }

            getProgressLayout().setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
            mListAdapter.setBusPosInfos(busPosInfos);
            mListAdapter.notifyDataSetChanged();
        }
    }
}