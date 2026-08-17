# Sound model

The first Android model will use a YAMNet TensorFlow Lite baseline.

Do not commit model binaries until the model is evaluated on recordings from the demo phone. The target labels are:

- `background`
- `alarm`
- `glass_break`

Later, a small classifier can be trained on YAMNet embeddings if the baseline needs tuning.
