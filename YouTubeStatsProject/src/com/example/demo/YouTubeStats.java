package com.example.demo;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;

public class YouTubeStats extends JFrame implements ActionListener {
    private static final String YOUTUBE_API_KEY = "AIzaSyAPnvPkcIBqAs6CkPYyvcKh9dLHQs5xhAE"; // My API key
    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/videos?part=snippet%%2Cstatistics%%2CcontentDetails&id=%s&key=%s";
    private JTextField urlTextField;
    private JButton getInfoButton;
    private JLabel titleLabel;
    private JLabel likesLabel;
    private JLabel commentsLabel;
    private JTextArea descriptionTextArea;
    private JLabel thumbnailUrlLabel;
    private JLabel thumbnailImageLabel;
    private JButton downloadThumbnailButton;
    private JPanel infoPanel;
    private JPanel thumbnailPanel;
    private String videoId;
    private String title;
    private String description;
    private String likes;
    private String comments;
    private String thumbnailUrl;
    private BufferedImage thumbnailImage;

    public YouTubeStats() {
        setTitle("YouTube Info & Thumbnail Downloader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        urlTextField = new JTextField(40);
        urlTextField.setToolTipText("Enter YouTube URL");

        getInfoButton = new JButton("Get Info");
        getInfoButton.addActionListener(this);
        // Attractive color for the "Get Info" button
        getInfoButton.setBackground(new Color(70, 130, 180)); // Steel Blue
        getInfoButton.setForeground(Color.WHITE);
        getInfoButton.setFocusPainted(false); // Remove focus border for cleaner look

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("YouTube URL:"));
        inputPanel.add(urlTextField);
        inputPanel.add(getInfoButton);
        add(inputPanel, BorderLayout.NORTH);

        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        titleLabel = new JLabel("Title:");
        likesLabel = new JLabel("Likes:");
        commentsLabel = new JLabel("Comments:");
        descriptionTextArea = new JTextArea("Description:", 5, 40);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setEditable(false);
        thumbnailUrlLabel = new JLabel("Thumbnail URL:");
        infoPanel.add(titleLabel);
        infoPanel.add(createSeparator());
        infoPanel.add(likesLabel);
        infoPanel.add(createSeparator());
        infoPanel.add(commentsLabel);
        infoPanel.add(createSeparator());
        infoPanel.add(new JLabel("Description:"));
        infoPanel.add(new JScrollPane(descriptionTextArea));
        infoPanel.add(createSeparator());
        infoPanel.add(thumbnailUrlLabel);
        infoPanel.add(createSeparator());

        add(new JScrollPane(infoPanel), BorderLayout.CENTER);

        thumbnailPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        thumbnailImageLabel = new JLabel();
        thumbnailPanel.add(thumbnailImageLabel);

        downloadThumbnailButton = new JButton("Download Thumbnail");
        downloadThumbnailButton.setEnabled(false);
        downloadThumbnailButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadThumbnailToFileChooser();
            }
        });
        thumbnailPanel.add(downloadThumbnailButton);

        add(thumbnailPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JSeparator createSeparator() {
        return new JSeparator(SwingConstants.HORIZONTAL);
    }

    // Method to extract video ID from the YouTube URL
    private String extractVideoId(String videoUrl) {
        Pattern pattern = Pattern.compile("(?<=watch\\?v=|/videos/|embed\\/|http(?:s)?://(?:www\\.)?youtu(?:\\.be/|be\\.com/watch\\?v=))([\\w-]+)");
        Matcher matcher = pattern.matcher(videoUrl);
        String extractedId = null;

        if (matcher.find()) {
            extractedId = matcher.group(1);
        } else {
            System.err.println("Warning: No YouTube video ID found in URL: " + videoUrl);
        }
        return extractedId;
    }

    // Method to fetch video statistics and snippet (including description) using YouTube API
    private void fetchVideoInfo(String videoUrl) {
        this.videoId = extractVideoId(videoUrl);
        if (this.videoId == null) {
            JOptionPane.showMessageDialog(this, "Invalid YouTube URL", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            URL url = new URL(String.format(YOUTUBE_API_URL, this.videoId, YOUTUBE_API_KEY));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != 200) {
                throw new IOException("API request failed with status code: " + connection.getResponseCode());
            }

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());

            // Extracting video title
            int titleStart = response.indexOf("\"title\": \"");
            if (titleStart != -1) {
                titleStart += 10;
                int titleEnd = response.indexOf("\",", titleStart);
                if (titleEnd != -1) {
                    this.title = response.substring(titleStart, titleEnd).replace("\\u0026", "&").replace("\\n", "\n");
                }
            }

            // Extracting like count
            int likeStart = response.indexOf("\"likeCount\": \"");
            if (likeStart != -1) {
                likeStart += 14;
                int likeEnd = response.indexOf("\",", likeStart);
                if (likeEnd != -1) {
                    this.likes = response.substring(likeStart, likeEnd);
                }
            }

            // Extracting comment count
            int statisticsStart = response.indexOf("\"statistics\":");
            if (statisticsStart != -1) {
                int commentCountStart = response.indexOf("\"commentCount\": \"", statisticsStart);
                if (commentCountStart != -1) {
                    commentCountStart += 18;
                    int commentCountEnd = response.indexOf("\"", commentCountStart);
                    if (commentCountEnd != -1) {
                        this.comments = response.substring(commentCountStart, commentCountEnd);
                    } else {
                        this.comments = "N/A";
                    }
                } else {
                    this.comments = "N/A";
                }
            } else {
                this.comments = "N/A";
            }

            // Extracting description
            int snippetStart = response.indexOf("\"snippet\":");
            if (snippetStart != -1) {
                int descriptionStart = response.indexOf("\"description\": \"", snippetStart);
                if (descriptionStart != -1) {
                    descriptionStart += 15;
                    int descriptionEnd = response.indexOf("\",", descriptionStart);
                    if (descriptionEnd != -1) {
                        this.description = response.substring(descriptionStart, descriptionEnd).replace("\\u0026", "&").replace("\\n", "\n");
                    } else {
                        this.description = "Description not found.";
                    }
                } else {
                    this.description = "Snippet data not found.";
                }
            } else {
                this.description = "Snippet data not found.";
            }

            // Extracting thumbnail URL
            int thumbStart = response.indexOf("\"url\": \"");
            if (thumbStart != -1) {
                thumbStart += 8;
                int thumbEnd = response.indexOf("\",", thumbStart);
                if (thumbEnd != -1) {
                    this.thumbnailUrl = response.substring(thumbStart, thumbEnd);
                    loadThumbnail();
                    downloadThumbnailButton.setEnabled(true);
                } else {
                    this.thumbnailUrl = null;
                    thumbnailImageLabel.setIcon(null);
                    downloadThumbnailButton.setEnabled(false);
                }
            } else {
                this.thumbnailUrl = null;
                thumbnailImageLabel.setIcon(null);
                downloadThumbnailButton.setEnabled(false);
            }

            displayInfo();

            inputStream.close();
            connection.disconnect();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error fetching video info: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadThumbnail() {
        if (this.thumbnailUrl != null) {
            try {
                URL url = new URL(this.thumbnailUrl);
                this.thumbnailImage = ImageIO.read(url);
                if (thumbnailImage != null) {
                    Image scaledImage = thumbnailImage.getScaledInstance(200, 150, Image.SCALE_SMOOTH);
                    thumbnailImageLabel.setIcon(new ImageIcon(scaledImage));
                } else {
                    thumbnailImageLabel.setIcon(new ImageIcon("error.png")); // Replace with your error image
                }
            } catch (IOException e) {
                thumbnailImageLabel.setIcon(new ImageIcon("error.png")); // Replace with your error image
            }
        } else {
            thumbnailImageLabel.setIcon(null);
        }
    }

    // Method to display the fetched video information
    public void displayInfo() {
        titleLabel.setText("Title: " + (title != null ? title : "N/A"));
        likesLabel.setText("Likes: " + (likes != null ? likes : "N/A"));
        commentsLabel.setText("Comments: " + (comments != null ? comments : "N/A"));
        descriptionTextArea.setText("Description: " + (description != null ? description : "N/A"));
        thumbnailUrlLabel.setText("Thumbnail URL: " + (thumbnailUrl != null ? thumbnailUrl : "N/A"));
    }

    private void downloadThumbnailToFileChooser() {
        if (this.thumbnailUrl != null && this.thumbnailImage != null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Thumbnail");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("JPEG files (*.jpg)", "jpg");
            fileChooser.setFileFilter(filter);

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".jpg")) {
                    filePath += ".jpg";
                    fileToSave = new File(filePath);
                }
                try {
                    ImageIO.write(thumbnailImage, "jpg", fileToSave);
                    JOptionPane.showMessageDialog(this, "Thumbnail downloaded successfully to:\n" + fileToSave.getAbsolutePath(), "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error saving thumbnail: " + e.getMessage(), "Download Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "No thumbnail available to download.", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == getInfoButton) {
            String videoUrl = urlTextField.getText();
            if (!videoUrl.isEmpty()) {
                fetchVideoInfo(videoUrl);
            } else {
                JOptionPane.showMessageDialog(this, "Please enter a YouTube URL.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(YouTubeStats::new);
    }
}
