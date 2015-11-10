package gtrj.shbz.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gtrj.shbz.R;
import gtrj.shbz.util.ContextString;
import gtrj.shbz.util.OkHttpUtil;
import gtrj.shbz.view.zTextView;

import static android.view.LayoutInflater.*;

public class PensionValidateListActivity extends BaseActivity implements View.OnClickListener {
    private zTextView all;
    private zTextView undo;
    private zTextView done;
    private ListView infoList;
    private TextView pageNum;
    private ProgressBarCircularIndeterminate loading;

    private List<Info> list;
    private List<Info> infoListData;
    private InfoListAdapter adapter;

    private String dataType = "2";
    private int page = 1;
    private int rows = 6;
    private int totalPage = 0;
    private String id = "";

    private Boolean clickable = true;

    private File tempPic;
    private String validateId;


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pension_validate_list);
        //处理5.0以上系统虚拟按键栏挡住按钮的问题
        getWindow().getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        loading = (ProgressBarCircularIndeterminate) findViewById(R.id.loading);
        pageNum = (TextView) findViewById(R.id.page_num);

        all = (zTextView) findViewById(R.id.all);
        undo = (zTextView) findViewById(R.id.undo);
        done = (zTextView) findViewById(R.id.done);
        all.setOnClickListener(this);
        undo.setOnClickListener(this);
        undo.click(true);
        done.setOnClickListener(this);

        Button prePage = (Button) findViewById(R.id.pre_page);
        Button nextPage = (Button) findViewById(R.id.next_page);
        prePage.setOnClickListener(this);
        nextPage.setOnClickListener(this);

        infoList = (ListView) findViewById(R.id.info_list);
        infoListData = new ArrayList<>();
        adapter = new InfoListAdapter(this, infoListData);
        infoList.setAdapter(adapter);
        infoList.setOnItemClickListener((parent, view, position, id1) -> {
            if (adapter.list.get(position).checkable) {
                if (Environment.MEDIA_MOUNTED.equals(Environment
                        .getExternalStorageState())) {
                    File filedir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "SHBZ");
                    if (!filedir.exists()) {
                        filedir.mkdirs();
                    }
                    tempPic = new File(filedir.getPath() + File.separator + "temp.jpg");
                    if (!tempPic.exists()) {
                        try {
                            tempPic.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    validateId = adapter.list.get(position).getId();
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(tempPic));

                    startActivityForResult(intent, 1);
                } else {
                    Toast.makeText(view.getContext(), "SD卡不可用", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(view.getContext(), "该用户已被验证", Toast.LENGTH_SHORT).show();
            }
        });
        handleIntent(getIntent());
        Message msg = msgHandler.obtainMessage();
        msg.arg1 = 1;
        msgHandler.sendMessage(msg);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, UploadPictureActivity.class);
            intent.putExtra("path", tempPic.getPath());
            intent.putExtra("Id", validateId);
            startActivity(intent);
        }
    }

    private final Handler msgHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case 1:
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            clickable = false;
                            SharedPreferences preferences = getSharedPreferences("SHBZ", 0);
                            String userName = preferences.getString("userName", "");
                            Infos infos = getData(userName, dataType, id);
                            if (infos != null) {
                                id = "";
                                list = infos.getList();
                                totalPage = infos.getTotal() % rows == 0 ? infos.getTotal() / rows : infos.getTotal() / rows + 1;
                                Message msg = msgHandler.obtainMessage();
                                msg.arg1 = 2;
                                msgHandler.sendMessage(msg);
                            } else {
                                list = new ArrayList<>();
                                totalPage = 1;
                                page = 1;
                                Message msg = msgHandler.obtainMessage();
                                msg.arg1 = 2;
                                msgHandler.sendMessage(msg);
                                id = "";
                            }
                        }
                    });
                    thread.start();
                    break;
                case 2:
                    pageNum.setText(page + "/" + totalPage + " 页");
                    infoListData.clear();
                    infoListData.addAll(list);
                    adapter.notifyDataSetChanged();
                    infoList.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.INVISIBLE);
                    clickable = true;
                    break;
                default:
                    break;
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onClick(View v) {
        if (!clickable) {
            return;
        }
        switch (v.getId()) {
            case R.id.all:
                clearSelection();
                setSelection(R.id.all);
                break;
            case R.id.undo:
                clearSelection();
                setSelection(R.id.undo);
                break;
            case R.id.done:
                clearSelection();
                setSelection(R.id.done);
                break;
            case R.id.pre_page:
                changePage(R.id.pre_page);
                break;
            case R.id.next_page:
                changePage(R.id.next_page);
                break;
        }
    }

    private void changePage(int viewId) {
        if (viewId == R.id.pre_page) {
            if (page > 1) {
                page--;
                sendMessage();
            }
        } else {
            if (page < totalPage) {
                page++;
                sendMessage();
            }
        }
    }

    private void sendMessage() {
        loading.setVisibility(View.VISIBLE);
        infoList.setVisibility(View.INVISIBLE);
        Message msg = msgHandler.obtainMessage();
        msg.arg1 = 1;
        msgHandler.sendMessage(msg);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setSelection(int viewId) {
        if (viewId == R.id.all) {
            dataType = "";
            page = 1;
            all.click(true);
            sendMessage();
        } else if (viewId == R.id.undo) {
            dataType = "2";
            page = 1;
            undo.click(true);
            sendMessage();
        } else {
            dataType = "1";
            page = 1;
            done.click(true);
            sendMessage();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void clearSelection() {
        all.click(false);
        undo.click(false);
        done.click(false);
    }

    private Infos getData(String username, String requestType, String id) {
        Map<String, String> map = new HashMap<>();
        map.put("userId", username);
        map.put("validation", requestType);
        map.put("id", id);
        map.put("rows", String.valueOf(rows));
        map.put("page", String.valueOf(page));
        try {
            //String result = HttpClientUtil.getData(ContextString.INFOLIST, map);
            //改用okhttp
            String result = OkHttpUtil.Post(ContextString.INFOLIST, map);
            Gson gson = new Gson();
            Type type = new TypeToken<Infos>() {
            }.getType();
            Infos infos = gson.fromJson(result, type);
            if (infos != null && infos.getTotal() != 0) {
                return infos;
            }
        } catch (OkHttpUtil.SessionOutOfTimeException e) {
            e.printStackTrace();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("isSessionOutOfTime", "1");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            this.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    //生成对应的菜单,并添加到Menu中
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search_btn).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        int searchTextViewId = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        TextView searchTextView = (TextView) searchView.findViewById(searchTextViewId);
        searchTextView.setHintTextColor(getResources().getColor(R.color.white));
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            id = intent.getStringExtra(SearchManager.QUERY);
            dataType = "";
            page = 1;
            Message msg = msgHandler.obtainMessage();
            msg.arg1 = 1;
            msgHandler.sendMessage(msg);
        } else if ("1".equals(intent.getStringExtra("finish"))) {
            sendMessage();
        }
    }

    public class InfoListAdapter extends BaseAdapter {
        private List<Info> list;
        private Context context;

        public InfoListAdapter(Context context, List<Info> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            InfoViewHolder infoViewHolder;
            if (convertView == null) {
                convertView = from(context).inflate(R.layout.info_list_item, null);
                infoViewHolder = new InfoViewHolder(convertView);
                convertView.setTag(infoViewHolder);
            } else {
                infoViewHolder = (InfoViewHolder) convertView.getTag();
            }
            infoViewHolder.infoName.setText(list.get(position).getName());
            infoViewHolder.infoId.setText(list.get(position).getId());
            infoViewHolder.infoId.setTextColor(Color.parseColor("#82858b"));
            infoViewHolder.infoArea.setText(list.get(position).getAreaName());
            infoViewHolder.infoArea.setTextColor(Color.parseColor("#82858b"));
            String sta = list.get(position).getValidation();
            String year = list.get(position).getCheckYear();
            String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
            if (currentYear.equals(year)) {
                switch (sta) {
                    case "0":
                    case "1":
                        infoViewHolder.state.setText("已验证");
                        infoViewHolder.state.setTextColor(Color.GREEN);
                        list.get(position).setCheckable(false);
                        break;
                    default:
                        infoViewHolder.state.setText("未验证");
                        infoViewHolder.state.setTextColor(Color.BLUE);
                        list.get(position).setCheckable(true);
                        break;
                }
            } else {
                infoViewHolder.state.setText("未验证");
                infoViewHolder.state.setTextColor(Color.RED);
                list.get(position).setCheckable(true);
            }

            return convertView;
        }

        public class InfoViewHolder {
            private TextView infoName;
            private TextView infoId;
            private TextView infoArea;
            private TextView state;

            public InfoViewHolder(View itemView) {
                infoName = (TextView) itemView.findViewById(R.id.info_name);
                infoId = (TextView) itemView.findViewById(R.id.info_id);
                infoArea = (TextView) itemView.findViewById(R.id.info_area);
                state = (TextView) itemView.findViewById(R.id.state);
            }
        }
    }

    /**
     * 为了便于gson处理而创建的class
     */
    public static class Infos {
        private int total;
        private List<Info> rows;

        public Infos(int total, List<Info> list) {
            this.total = total;
            this.rows = list;
        }

        public int getTotal() {
            return total;
        }

        public List<Info> getList() {
            return rows;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public void setList(List<Info> list) {
            this.rows = list;
        }
    }

    /**
     * 用户信息pojo类
     */
    public class Info {
        private String id; //身份证
        private String name; //姓名
        private String areaName; //参保社区
        private String validation; //验证结果
        private String checkYear; //验证时间
        private Boolean checkable; //是否可验证

        public Info(String id, String name, String areaName) {
            this.id = id;
            this.name = name;
            this.areaName = areaName;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAreaName() {
            return areaName;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAreaName(String areaName) {
            this.areaName = areaName;
        }

        public void setValidation(String validation) {
            this.validation = validation;
        }

        public void setCheckYear(String checkYear) {
            this.checkYear = checkYear;
        }

        public String getValidation() {
            return validation;
        }

        public String getCheckYear() {
            return checkYear;
        }

        public void setCheckable(Boolean checkable) {
            this.checkable = checkable;
        }

        public Boolean isCheckable() {

            return checkable;
        }

        @Override
        public String toString() {
            return "Info{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", areaName='" + areaName + '\'' +
                    ", validation='" + validation + '\'' +
                    ", checkYear='" + checkYear + '\'' +
                    '}';
        }
    }
}
