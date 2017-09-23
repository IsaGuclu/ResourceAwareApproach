options( java.parameters = "-Xmx4g" )
getwd()
setwd("C:/ProgramExt/WS-R/TrOWL_1122")
getwd()

library(plyr)						# Progress bar
library(XLConnect)			# Using Ms Excel data

split_str_by_index <- function(target, index) {
  index <- sort(index)
  substr(rep(target, length(index) + 1),
         start = c(1, index),
         stop = c(index -1, nchar(target)))
}

#Taken from https://stat.ethz.ch/pipermail/r-help/2006-March/101023.html
interleave <- function(v1,v2)
{
  ord1 <- 2*(1:length(v1))-1
  ord2 <- 2*(1:length(v2))
  c(v1,v2)[order(c(ord1,ord2))]
}

insert_str <- function(target, insert, index) {
  insert <- insert[order(index)]
  index <- sort(index)
  paste(interleave(split_str_by_index(target, index), insert), collapse="")
}



f <- function (file) {
  ## read file
  myData <- read.csv(file)
 
  ## make a plot
  plot(msec ~ sizePC, xlab="Sample from Ontology with Ratio (%)", ylab="Execution Time of a Sample (msec.)", 
       data = myData, main=insert_str(tools::file_path_sans_ext(file),"\n",81))  ## use file names as title
  # main = file
  ## fit a linear model
  model1 <- lm(msec ~ sizePC, data = myData)
  # summary(model1)
  abline(model1, lwd=5, col="orange") ## overlay fitted regression line
  
  ## fit poly^2 model
  model2 <- lm(myData$msec ~ poly(myData$sizePC, degree=2, raw=T))
  # summary(model2)
  lines(smooth.spline(myData$sizePC, predict(model2)), col="blue", lwd=3)
  
  ## fit poly^3 model
  model3 <- lm(myData$msec ~ poly(myData$sizePC, degree=3, raw=T))
  # summary(model3)
  lines(smooth.spline(myData$sizePC, predict(model3)), col="red", lwd=2)

  # Test with 100% (original) ontology
  myData <- data.frame(sizePC=c(100)) # read.csv("originalOntology/original.csv")
  
  # originalOntlogy <- data.frame(sizePC=c(100), msec=c(0))
  y_lin = predict(model1, newdata = myData)
  y_Poly2 = predict(model2, newdata = myData)
  y_Poly3 = predict(model3, newdata = myData)
  
  fileConn<-file(paste("prediction4original/" , basename(file), ".txt", sep="")) # ".txt",
  writeLines(c(paste("CSVFile:",basename(file)),paste("linear:", y_lin),paste("polynomial2:", y_Poly2),paste("polynomial3:", y_Poly3)), fileConn)
  close(fileConn)

  }
  
temp <- list.files(pattern = "*.csv")
pdf("1122_Models_Generated_From_10_Samples_On_TrOWL.pdf", width=9) # 
result <- t(sapply(temp, f))
dev.off()
