package app.example.capriscan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    private ArrayList<Announcement> announcements;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private final OnAnnouncementClickListener listener;

    public interface OnAnnouncementClickListener {
        void onEditAnnouncement(Announcement announcement);
        void onDeleteAnnouncement(Announcement announcement);
    }

    public AnnouncementAdapter(ArrayList<Announcement> announcements, OnAnnouncementClickListener listener) {
        this.announcements = announcements;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement announcement = announcements.get(position);
        holder.titleTextView.setText(announcement.getTitle());
        holder.contentTextView.setText(announcement.getContent());
        holder.timestampTextView.setText(dateFormat.format(new Date(announcement.getTimestamp())));

        holder.editButton.setOnClickListener(v -> listener.onEditAnnouncement(announcement));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteAnnouncement(announcement));
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    public void updateAnnouncements(ArrayList<Announcement> newAnnouncements) {
        announcements.clear();
        announcements.addAll(newAnnouncements);
        notifyDataSetChanged();
    }

    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        TextView timestampTextView;
        ImageButton editButton;
        ImageButton deleteButton;

        AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.announcement_title);
            contentTextView = itemView.findViewById(R.id.announcement_content);
            timestampTextView = itemView.findViewById(R.id.announcement_timestamp);
            editButton = itemView.findViewById(R.id.edit_announcement_button);
            deleteButton = itemView.findViewById(R.id.delete_announcement_button);
        }
    }
}
