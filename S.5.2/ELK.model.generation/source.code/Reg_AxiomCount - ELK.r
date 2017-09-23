options( java.parameters = "-Xmx4g" )
getwd()
setwd("C:/ProgramExt/WS-R/SnomedCT_ELK")
getwd()

pdf("SnomedCT_10_Samples_ELK.pdf", width=9) # 

## read file
myData <- read.csv("snomed_jan17.owl_10Samples_ELK.csv")
 
## make a plot
plot(AxiomCount ~ msec, ylab="Axiom Count of Sample", xlab="Execution Time of a Sample (msec.)", 
       data = myData, main="10 Samples of SnomedCT on ELK")  ## use file names as title

## fit a linear model
model1 <- lm(AxiomCount ~ msec, data = myData)
# summary(model1)
abline(model1, lwd=3, col="orange") ## overlay fitted regression line

## fit poly^2 model
# model2 <- lm(myData$AxiomCount ~ poly(myData$msec, degree=2, raw=T))
# summary(model2)
# lines(smooth.spline(myData$msec, predict(model2)), col="blue", lwd=3)

## fit poly^3 model
# model3 <- lm(myData$AxiomCount ~ poly(myData$msec, degree=3, raw=T))
# summary(model3)
# lines(smooth.spline(myData$msec, predict(model3)), col="red", lwd=2)

# Test with 10 sec (10,000 msec)
myData <- data.frame(msec=c(10000))
y_10sec = predict(model1, newdata = myData)

# Test with 30 sec (30,000 msec)
myData <- data.frame(msec=c(30000))
y_30sec = predict(model1, newdata = myData)

# Test with 60 sec (60,000 msec)
myData <- data.frame(msec=c(60000))
y_60sec = predict(model1, newdata = myData)

fileConn<-file("prediction4thresholds/SnomedCT_Predictions_From_10_Samples_For_ELK.txt")
writeLines(c(paste("CSVFile:","snomed_jan17.owl"),paste("10sec:", y_10sec),paste("30sec:", y_30sec),paste("60sec:", y_60sec)), fileConn)
close(fileConn)

dev.off()
