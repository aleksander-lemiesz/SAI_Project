title Running three approval applications

start "Running Software approval..." cmd /c gradlew :approval:run --args "Grad.CoordinatorSoftware softwareRequests"
start "Running Technology approval..." cmd /c gradlew :approval:run --args "Grad.CoordinatorTechnology technologyRequests"
start "Running ExamBoard approval..." cmd /c gradlew :approval:run --args "ExamBoard examBoardRequests"

exit