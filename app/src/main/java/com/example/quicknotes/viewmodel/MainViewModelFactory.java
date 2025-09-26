package com.example.quicknotes.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.quicknotes.model.OnboardingRepository;
import com.example.quicknotes.model.PermissionRepository;

public class MainViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public MainViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            OnboardingRepository onboardingRepository = new OnboardingRepository(application.getApplicationContext());
            PermissionRepository permissionRepository = new PermissionRepository(application.getApplicationContext());
            return (T) new MainViewModel(onboardingRepository, permissionRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
