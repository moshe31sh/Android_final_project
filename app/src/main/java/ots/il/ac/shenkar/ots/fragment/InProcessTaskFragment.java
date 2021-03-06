package ots.il.ac.shenkar.ots.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import ots.il.ac.shenkar.ots.R;
import ots.il.ac.shenkar.ots.apputiles.AppConst;
import ots.il.ac.shenkar.ots.apputiles.AppUtils;
import ots.il.ac.shenkar.ots.common.Task;
import ots.il.ac.shenkar.ots.common.User;
import ots.il.ac.shenkar.ots.controlers.AppController;
import ots.il.ac.shenkar.ots.controlers.TasksFragmentAdapter;
import ots.il.ac.shenkar.ots.listeners.ItemClickListener;
import ots.il.ac.shenkar.ots.listeners.ItemLongClickListener;

/**
 * Created by moshe on 26-02-16.
 */
public class InProcessTaskFragment extends Fragment implements ItemLongClickListener,
        ItemClickListener {
    private AppController mController;
    private RecyclerView mRecyclerView;
    private TasksFragmentAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private View mView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private List<Task> taskList;
    private ProgressDialog mProgress;
    private Menu menu;
    private Animation animAlpha;
    private Task currentTask;
    private TextView mTvEmptyWaitingList , waitingTask;
    private CloudRefreshReceiver r;
    private User user;
    private Bitmap imageBitmap;
    private boolean takePic;
    private List <Task> toDeleteList;
    ////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////View task dialog vars///////////////////////////
    private TextView mTitle, mNote, mCategory, mTaskTime, mTaskDate, mPriority, mStatus,
            mEmployeeTvCurrentUser , mEmployeeTv;
    private Button mBtnOk, mBtnCancel ,mBtnSave;
    private ArrayAdapter mCategoryAdapter , mPriorityAdapter , mStatusAdapter ;
    private Spinner mCategorySpinner , mPrioritySpinner ,mStatusSpinner;
    private ImageButton mIbCamera;
    private ImageView mIvTaskPic;
    private RelativeLayout mRelativeLayoutTaskPic , mRelativeLayoutEmployee , mRelativeLayoutWaiting;

    /////////////////////////////////////////////////////////////////////////////////

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_layout, container, false);
        user = AppUtils.createUserFromPars(ParseUser.getCurrentUser());
        setHasOptionsMenu(true);
        if(user.getIsManager()){
            toDeleteList = new ArrayList<>();
        }
        init();
        return mView;

    }



    public void init() {
        takePic = false;
        animAlpha = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_alpha);
        mController = new AppController(this.getContext());
        mTvEmptyWaitingList = (TextView) mView.findViewById(R.id.id_waiting_empty_list);
        waitingTask = (TextView) mView.findViewById(R.id.id_task_number);
        mTvEmptyWaitingList.setVisibility(View.INVISIBLE);
        mRecyclerView = (RecyclerView) mView.findViewById(R.id.id_fragment_recycle_view);
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mView.findViewById(R.id.id_fragment_swipe);
        mProgress = new ProgressDialog(getActivity());

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(false);

                show();
            }
        });
        show();
    }



    public void  show(){

        taskList = mController.getTasksListByStatus(AppConst.IN_PROCESS);
        Integer numOfTask = mController.getTasksListByStatus(AppConst.WAITING).size();
        waitingTask.setText(numOfTask.toString());
        mAdapter = new TasksFragmentAdapter(taskList, getContext());
        mAdapter.setmItemClickListener(this);
        mAdapter.setmItemLongClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }





    @Override
    public void onItemClick(View view, int pos) {
        view.startAnimation(animAlpha);
        showTaskContent(taskList.get(pos));
    }




    @Override
    public void onItemLongClick(View view, int pos) {
        if (user.getIsManager()) {
            view.startAnimation(animAlpha);
            MenuItem item = menu.getItem(2);
            if(!toDeleteList.contains(taskList.get(pos))){
                toDeleteList.add(taskList.get(pos));
            }else{
                toDeleteList.remove(taskList.get(pos));
            }

            if(toDeleteList.size() > 0) {
                item.setVisible(true);
            }else{
                item.setVisible(false);
            }
        }
    }


    public void showTaskContent(final Task task) {
        currentTask = task;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setCancelable(true);
        final AlertDialog dialog = builder.create();
        View dialogView = dialog.getLayoutInflater().inflate(R.layout.view_task_page, null);
        dialog.setView(dialogView);
        mRelativeLayoutTaskPic = (RelativeLayout)dialogView.findViewById(R.id.task_view_pic_rel);
        mRelativeLayoutTaskPic.setVisibility(View.GONE);
        mRelativeLayoutWaiting = (RelativeLayout)dialogView.findViewById(R.id.id_task_view_waiting_rel);
        mRelativeLayoutWaiting.setVisibility(View.GONE);
        mRelativeLayoutEmployee = (RelativeLayout)dialogView.findViewById(R.id.id_task_view_employee);
        mIbCamera = (ImageButton) dialogView.findViewById(R.id.id_task_view_camera);
        mEmployeeTv = (TextView) dialogView.findViewById(R.id.id_employee_tv);
        mStatus = (TextView) dialogView.findViewById(R.id.id_view_task_status);
        mTitle = (TextView) dialogView.findViewById(R.id.id_view_task_title);
        mNote = (TextView) dialogView.findViewById(R.id.id_view_task_notes);
        mCategory = (TextView) dialogView.findViewById(R.id.id_view_task_category);
        mTaskTime = (TextView) dialogView.findViewById(R.id.id_view_task_time);
        mTaskDate = (TextView) dialogView.findViewById(R.id.id_view_task_date);
        mPriority = (TextView) dialogView.findViewById(R.id.id_view_task_priority);
        mBtnOk = (Button) dialogView.findViewById(R.id.id_view_task_ok_button);
        mBtnCancel = (Button) dialogView.findViewById(R.id.id_view_task_cancel_button);
        mBtnSave = (Button) dialogView.findViewById(R.id.id_view_task_save_button);
        mEmployeeTvCurrentUser = (TextView) dialogView.findViewById(R.id.id_view_task_employee);
        mCategorySpinner = (Spinner) dialogView.findViewById(R.id.Category_array);
        mPrioritySpinner = (Spinner) dialogView.findViewById(R.id.Priority_array);
        mStatusSpinner = (Spinner) dialogView.findViewById(R.id.id_status_array);
        mIvTaskPic = (ImageView) dialogView.findViewById(R.id.id_task_view_pic);

        mStatusSpinner.setVisibility(View.INVISIBLE);
        mCategorySpinner.setVisibility(View.INVISIBLE);
        mPrioritySpinner.setVisibility(View.INVISIBLE);
        mBtnCancel.setVisibility(View.INVISIBLE);
        mBtnOk.setVisibility(View.INVISIBLE);
        mBtnSave.setVisibility(View.INVISIBLE);
        mStatusSpinner.setVisibility(View.INVISIBLE);

        mIbCamera.setVisibility(View.INVISIBLE);

        if(user.getIsManager() == true) {
            mBtnSave.setVisibility(View.VISIBLE);
            mBtnCancel.setVisibility(View.VISIBLE);
            mCategorySpinner.setVisibility(View.VISIBLE);
            mPrioritySpinner.setVisibility(View.VISIBLE);
            mCategory.setVisibility(View.INVISIBLE);
            mPriority.setVisibility(View.INVISIBLE);
            mCategoryAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.category, R.layout.spinner_layout);
            mPriorityAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.priority, R.layout.spinner_layout);
            mCategorySpinner.setAdapter(mCategoryAdapter);
            mPrioritySpinner.setAdapter(mPriorityAdapter);
            mCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    TextView textView = (TextView) view;
                    currentTask.setCategory(textView.getText().toString());

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            mPrioritySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    TextView textView = (TextView) view;
                    currentTask.setPriority(textView.getText().toString());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            mBtnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUtils.Toast(getContext(), AppConst.DISCARD_CHANGES);
                    dialog.dismiss();
                }
            });

            mBtnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUtils.Toast(getContext(), AppConst.SAVE_CHANGES);
                    dialog.dismiss();
                    mProgress.setMessage("update tasks data...");
                    mProgress.show();
                    currentTask.setFirstRead(1);
                    currentTask.setFirstSync(1);
                    mController.updateTaskLocalDb(currentTask);
                    mController.updateTask(currentTask, new FindCallback() {
                        @Override
                        public void done(List objects, ParseException e) {
                        }

                        @Override
                        public void done(Object o, Throwable throwable) {
                            mProgress.dismiss();
                            dialog.dismiss();
                            mController.notifyAllOnChanges();
                        }
                    });
                }
            });
////if employee
        }else{
            mRelativeLayoutEmployee.setVisibility(View.GONE);
            mStatus.setVisibility(View.INVISIBLE);
            mEmployeeTv.setVisibility(View.INVISIBLE);
            mEmployeeTvCurrentUser.setVisibility(View.INVISIBLE);

            mBtnCancel.setVisibility(View.VISIBLE);
            mBtnSave.setVisibility(View.VISIBLE);
            mStatusSpinner.setVisibility(View.VISIBLE);
            mStatusAdapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.status, R.layout.spinner_layout);
            mStatusSpinner.setAdapter(mStatusAdapter);
            mStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    TextView textView = (TextView) view;
                    currentTask.setTaskStatus(textView.getText().toString());
                    if (textView.getText().toString().equals("Done")) {
                        mIbCamera.setVisibility(View.VISIBLE);
                        mIbCamera.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                takePic();
                            }
                        });
                    } else {
                        mIbCamera.setVisibility(View.INVISIBLE);
                        mRelativeLayoutTaskPic.setVisibility(View.GONE);


                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            mBtnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUtils.Toast(getContext(), AppConst.DISCARD_CHANGES);
                    if (currentTask.isFirstRead() == 1) {
                        currentTask.setFirstRead(0);
                        mController.updateTaskLocalDb(currentTask);
                        mController.updateTask(currentTask, new FindCallback() {
                            @Override
                            public void done(List objects, ParseException e) {

                            }

                            @Override
                            public void done(Object o, Throwable throwable) {

                            }
                        });
                    }
                    mController.notifyAllOnChanges();
                    dialog.cancel();
                }
            });
            mBtnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentTask.getTaskStatus().equals(AppConst.DONE) && takePic == true) {
                        mProgress.setMessage("update tasks data...");
                        mProgress.show();
                        mController.updateTaskLocalDb(currentTask);
                        mController.updateTask(currentTask, new FindCallback() {
                            @Override
                            public void done(List objects, ParseException e) {

                            }

                            @Override
                            public void done(Object o, Throwable throwable) {
                                mProgress.dismiss();
                                dialog.dismiss();
                            }
                        });
                        mController.notifyAllOnChanges();
                    } else {
                        AppUtils.Toast(getContext() , AppConst.MOST_TAKE_PIC);
                    }
                }
            });

        }
        mTitle.setText(currentTask.getTitle());
        mNote.setText(currentTask.getContent());
        mCategory.setText(currentTask.getCategory());
        mEmployeeTvCurrentUser.setText(currentTask.getUser());
        mTaskTime.setText(currentTask.getTime());
        mTaskDate.setText(currentTask.getDate());
        mPriority.setText(currentTask.getPriority());
        mStatus.setText(currentTask.getTaskStatus());
        dialog.show();


    }

    public void refresh() {
        show();
        if(toDeleteList != null) {
            toDeleteList.clear();
        }
    }

    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(r);
    }

    public void onResume() {
        super.onResume();
        r = new CloudRefreshReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(r,
                new IntentFilter("TAG_REFRESH"));
    }



    private class CloudRefreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            InProcessTaskFragment.this.refresh();
        }
    }


    /**
     * this method start camera
     */
    private void takePic(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, AppConst.REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == AppConst.REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                imageBitmap = (Bitmap) extras.get("data");
                if(imageBitmap != null) {
                    mRelativeLayoutTaskPic.setVisibility(View.VISIBLE);
                    mIvTaskPic.setImageBitmap(imageBitmap);
                    currentTask.setDoneTaskPic(AppUtils.createBytesFromImage(imageBitmap));
                    takePic = true;
                }
            }

        } catch (Exception e) {

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (user.getIsManager()) {
            super.onCreateOptionsMenu(menu, inflater);
            this.menu = menu;
            MenuItem item = menu.getItem(2);
            item.setVisible(false);
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (user.getIsManager()) {
            switch (item.getItemId()) {
                case R.id.id_action_manager_delete:
                    // Not implemented here
                    mController.deleteTasksDialog(toDeleteList);
                    item.setVisible(false);
                    show();
                    return false;
                default:
                    break;
            }
        }
        return false;
    }
}




