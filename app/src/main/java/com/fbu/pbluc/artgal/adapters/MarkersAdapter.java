package com.fbu.pbluc.artgal.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fbu.pbluc.artgal.R;
import com.fbu.pbluc.artgal.models.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class MarkersAdapter extends RecyclerView.Adapter<MarkersAdapter.ViewHolder> {

    private List<Marker> mMarkers;
    private Context mContext;

    public MarkersAdapter(List<Marker> mMarkers, Context mContext) {
        this.mMarkers = mMarkers;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_marker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MarkersAdapter.ViewHolder holder, int position) {
        Marker marker = mMarkers.get(position);
        holder.bind(marker);
    }

    @Override
    public int getItemCount() {
        return mMarkers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private FirebaseStorage firebaseStorage;

        private TextView tvTitle;
        private TextView tvDescription;
        private TextView tvAugmentedObjectFileName;
        private TextView tvCreatedAt;
        private ImageView ivReferenceImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAugmentedObjectFileName = itemView.findViewById(R.id.tvAugmentedObjectFileName);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            ivReferenceImage = itemView.findViewById(R.id.ivReferenceImg);

            firebaseStorage = FirebaseStorage.getInstance();
        }

        public void bind(Marker marker) {
            tvTitle.setText(marker.getTitle());
            tvDescription.setText(marker.getDescription());
            tvAugmentedObjectFileName.setText(marker.getAugmentedObj().get("fileName").toString().substring(49));
            tvCreatedAt.setText(marker.calculateTimeAgo());

            StorageReference storageReference = firebaseStorage.getReference();
            StorageReference markerImgReference = storageReference.child("referenceImages/" + marker.getMarkerImg().get("fileName").toString());

            markerImgReference
                    .getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Glide.with(mContext).load(uri).into(ivReferenceImage);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("MarkersAdapter", "Could not get download url of marker img", e);
                        }
                    });
        }
    }
}
