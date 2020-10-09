package software.engineering2.mockandroid.home;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import software.engineering2.mockandroid.AppManager;
import software.engineering2.mockandroid.R;
import software.engineering2.mockandroid.ResponseUpdatable;
import software.engineering2.mockandroid.models.MultiViewTypeAdapter;
import software.engineering2.mockandroid.models.TemplateModel;
import software.engineering2.mockandroid.models.gadgets.Gadget_basic;

public class HomeFragment extends Fragment implements ResponseUpdatable {

    private static final String TAG = "Info";
    private NavController navController;

    private Button loginBtn;
    private RecyclerView recyclerView;
    private MultiViewTypeAdapter multiViewTypeAdapter;
    private ArrayList<TemplateModel> gadgetCards;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        getActivity().getWindow().setStatusBarColor(getActivity().getColor(R.color.colorPrimaryDark));
        Log.d(TAG, "In the HomeFragment");
        AppManager.getInstance().currentFragmentView = this;
        gadgetCards = new ArrayList<>();
        recyclerView = view.findViewById(R.id.gadgetList);
        loginBtn = view.findViewById(R.id.LoginBtn);


        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppManager.getInstance().requestToServer("101::Sebastian::ffff");
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // will create recyclerview in linearlayoyt
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return view;
    }

    @Override
    public void update(int protocolIndex, String message) {
        switch (protocolIndex) {
            case 304:
                for(Map.Entry<Integer, Gadget_basic> entry : AppManager.getInstance().getGadgets().entrySet()) {
                    switch (entry.getValue().type){
                        case SWITCH:
                            gadgetCards.add(new TemplateModel(TemplateModel.SWITCH_CARD));
                            break;
                        case BINARY_SENSOR:
                            gadgetCards.add(new TemplateModel(TemplateModel.BINARY_SENSOR_CARD));
                            break;
                        case SENSOR:
                            gadgetCards.add(new TemplateModel(TemplateModel.SENSOR_CARD));
                            break;
                        case SET_VALUE:

                            break;
                    }
                }
                multiViewTypeAdapter = new MultiViewTypeAdapter(getContext(),gadgetCards);
                recyclerView.setAdapter(multiViewTypeAdapter);
                multiViewTypeAdapter.notifyDataSetChanged();
                break;
            case 316:
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                multiViewTypeAdapter.notifyDataSetChanged();
                break;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "HomeFragment: In the onDestroyView() event");
    }

    // 1
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "HomeFragment: In the onAttach() event");
    }

    //2
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HomeFragment: In the OnCreate event()");
        // This callback will only be called when Fragment is at least Started.
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                getActivity().finish();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
        // The callback can be enabled or disabled here or in handleOnBackPressed()
    }

    //4
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "HomeFragment: In the onActivityCreated() event");
    }

    //5
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "HomeFragment: In the onStart() event");
    }

    //6
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "HomeFragment: In the onResume() event");
        AppManager.getInstance().createServerConnection();

    }

    //7
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "HomeFragment: In the onPause() event");
        gadgetCards.clear();
    }

    //8
    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "HomeFragment: In the onStop() event");
    }

    //10
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "HomeFragment: In the onDestroy() event");
    }

    //11
    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "HomeFragment: In the onDetach() event");
    }

}


