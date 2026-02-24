# s3checksum

Verifies the integrity of files stored in an S3 bucket by comparing their MD5 checksums against a control CSV.

## Requirements

- Java 17+
- Maven

## Environment Variables

| Variable | Description |
|---|---|
| `inputFilePath` | Path to the input CSV file |
| `bucketName` | Name of the S3 bucket |
| `region` | AWS region (e.g. `us-east-1`) |
| `accessKey` | AWS access key ID |
| `secretKey` | AWS secret access key |

## Input CSV Format

The input CSV must have a header row and the following columns:

```
folderId,fileName,md5,size
```

- `folderId` - The S3 key prefix (folder path) for the object
- `fileName` - The object name within the folder
- `md5` - The expected MD5 hash
- `size` - The file size

The full S3 key is constructed as `folderId/fileName`.

## Usage

```bash
# Build
mvn clean package

# Run
inputFilePath=data.csv \
bucketName=my-bucket \
region=us-east-1 \
accessKey=AKIA... \
secretKey=... \
java -jar target/s3checksum-1.0.jar
```

### Running from IntelliJ

1. Open the project in IntelliJ
2. Go to **Run > Edit Configurations**
3. Create a new **Application** configuration
4. Set the main class to `com.contractpal.s3checksum.Main`
5. Under **Environment variables**, click the icon and add:
   - `inputFilePath` = path to your CSV
   - `bucketName` = your S3 bucket name
   - `region` = your AWS region
   - `accessKey` = your AWS access key
   - `secretKey` = your AWS secret key
6. Click **Apply** and **Run**

## Output

Results are written to `output/s3-checksum.csv` with columns:

```
folderId,filename,size,md5,calculatedMd5,verified,comments
```