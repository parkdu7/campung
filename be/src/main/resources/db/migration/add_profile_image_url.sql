-- Add profile_image_url column to user table
ALTER TABLE user ADD COLUMN profile_image_url VARCHAR(512) NULL AFTER nickname;

-- Add comment for the new column
ALTER TABLE user MODIFY COLUMN profile_image_url VARCHAR(512) NULL COMMENT 'S3에 저장된 프로필 이미지 URL';