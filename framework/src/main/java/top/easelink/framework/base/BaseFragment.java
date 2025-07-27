package top.easelink.framework.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.viewbinding.ViewBinding;

import org.jetbrains.annotations.NotNull;

import top.easelink.framework.topbase.ControllableFragment;
import top.easelink.framework.topbase.TopActivity;

public abstract class BaseFragment<T extends ViewBinding, V extends ViewModel> extends Fragment implements ControllableFragment {

    private T viewBinding;

    @Override
    public boolean isControllable() {
        return true;
    }

    @NotNull
    @Override
    public String getBackStackTag() {
        return this.getClass().getSimpleName();
    }

    @LayoutRes
    public abstract int getLayoutId();

    public abstract V getViewModel();

    protected abstract T initViewBinding(@NonNull LayoutInflater inflater, ViewGroup container);

    @Override
    @SuppressWarnings("rawtypes")
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);

        if (context instanceof AppCompatActivity) {

            if (this.isControllable()) {
                if (context instanceof BaseActivity) {
                    ((BaseActivity) context).onFragmentAttached(getBackStackTag());
                } else if (context instanceof TopActivity) {
                    ((TopActivity) context).onFragmentAttached(getBackStackTag());
                }
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getViewModel();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = initViewBinding(inflater, container);
        return viewBinding.getRoot();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }
}