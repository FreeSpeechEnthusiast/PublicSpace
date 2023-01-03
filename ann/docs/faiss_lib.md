.. \_faiss_lib:

# Faiss ANN Library

[Faiss](https://github.com/facebookresearch/faiss) is an opensource library of algorithms developed by Facebook. Library provide wide range of tradeoffs for solving ANN problem, suitable for various domains.

There are multiple indexing strategies available. It's possible to apply preprocessing steps, such as PCA or rotations.

Faiss has capability to combine indexes and preprocessing steps together to achieve better efficiency. One of the ways to achieve this is to use [factory string API](https://github.com/facebookresearch/faiss/wiki/The-index-factory)

## Overview

- Faiss is a library allowing many indexing strategies
- There is no single indexing strategy which works well for all usecases
- Library is written in c++ which generally has better performance

https://github.com/facebookresearch/faiss/wiki/Faiss-indexes
https://github.com/facebookresearch/faiss/wiki/Pre--and-post-processing#pre-transforming-the-data
