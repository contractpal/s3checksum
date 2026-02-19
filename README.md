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

## Output

Results are written to `output/s3-checksum.csv` with columns:

```
folderId,filename,size,md5,calculatedMd5,verified,comments
```

Objects are verified concurrently using 50 threads with streaming downloads to minimize memory usage.